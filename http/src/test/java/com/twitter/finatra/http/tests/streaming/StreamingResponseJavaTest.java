package com.twitter.finatra.http.tests.streaming;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import scala.collection.JavaConverters;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.twitter.concurrent.AsyncStream;
import com.twitter.finagle.http.Response;
import com.twitter.finagle.stats.StatsReceiver;
import com.twitter.finatra.http.internal.marshalling.MessageBodyManager;
import com.twitter.finatra.http.response.ResponseBuilder;
import com.twitter.finatra.http.streaming.StreamingResponse;
import com.twitter.finatra.http.streaming.ToReader;
import com.twitter.finatra.json.FinatraObjectMapper;
import com.twitter.finatra.utils.FileResolver;

import com.twitter.io.Buf;
import com.twitter.io.Bufs;
import com.twitter.io.Reader;
import com.twitter.io.Readers;
import com.twitter.io.StreamTermination;
import com.twitter.util.Await;
import com.twitter.util.Function;
import com.twitter.util.Future;

public class StreamingResponseJavaTest extends Assert {

  private Future burnLoop(Reader<Buf> reader) {
    return reader.read().flatMap(Function.func(v1 -> {
      if (v1.nonEmpty()) {
        return burnLoop(reader);
      } else {
        return Future.Unit();
      }
    }));
  }

  private ResponseBuilder responseBuilder = new ResponseBuilder(
    FinatraObjectMapper.create(null),
    new FileResolver("src/main/webapp/", ""),
    Mockito.mock(MessageBodyManager.class),
    Mockito.mock(StatsReceiver.class),
    true
  );

  private Response fromReader(Reader reader) throws Exception {
    StreamingResponse streamingResponse = responseBuilder.streaming(
      reader, ToReader.ReaderIdentity());
    Future<Response> fResponse = streamingResponse.toFutureResponse();
    return Await.result(fResponse);
  }

  private Reader<Buf> infiniteReader(Buf buf) {
    Stream<Buf> infiniteStream = Stream.iterate(buf, i -> i.concat(buf));
    return Readers.fromSeq(infiniteStream);
  }

  @Test
  public void emptyReader() throws Exception {
    Reader reader = Readers.newEmptyReader();
    Response response = fromReader(reader);
    // observe the EOS
    response.reader().read();
    Assert.assertEquals(
      Await.result(response.reader().onClose()), StreamTermination.FullyRead$.MODULE$);
  }

  @Test
  public void serdeReaderofString() throws Exception {
    List<String> stringList = Arrays.asList("first", "second", "third");
    Reader<String> reader = Readers.fromSeq(stringList);
    Response response = fromReader(reader);
    Future<Buf> fBuf = Readers.readAll(response.reader());
    String result = Buf.decodeString(Await.result(fBuf), StandardCharsets.UTF_8);
    Assert.assertEquals("\"first\"\"second\"\"third\"", result);
  }

  @Test
  public void serdeReaderofObject() throws Exception {
    FooClass f1 = new FooClass(1, "first");
    FooClass f2 = new FooClass(2, "second");
    List<FooClass> fooList = Arrays.asList(f1, f2);
    Reader<FooClass> reader = Readers.fromSeq(fooList);
    Response response = fromReader(reader);
    Future<Buf> fBuf = Readers.readAll(response.reader());
    String result = Buf.decodeString(Await.result(fBuf), StandardCharsets.UTF_8);
    Assert.assertEquals("{\"v1\":1,\"v2\":\"first\"}{\"v1\":2,\"v2\":\"second\"}", result);
  }

  @Test
  public void readerWriteFailureWithReaderDicardedException() throws Exception {
    Reader<Buf> reader = infiniteReader(Bufs.UTF_8.apply("foo"));
    Response response = fromReader(reader);
    response.reader().discard();
    burnLoop(response.reader());
    Assert.assertEquals(
      Await.result(response.reader().onClose()), StreamTermination.Discarded$.MODULE$);
  }

  private Response fromStream(AsyncStream stream) throws Exception {
    StreamingResponse streamingResponse = responseBuilder.streaming(
      stream, ToReader.AsyncStreamToReader());
    Future<Response> fResponse = streamingResponse.toFutureResponse();
    return Await.result(fResponse);
  }

  @Test
  public void serdeAsyncStreamOfString() throws Exception {
    List<String> stringList = Arrays.asList("first", "second", "third");
    AsyncStream<String> stream = AsyncStream.fromSeq(
      JavaConverters.asScalaIteratorConverter(stringList.iterator()).asScala().toSeq());
    Response response = fromStream(stream);
    Future<Buf> fBuf = Readers.readAll(response.reader());
    String result = Buf.decodeString(Await.result(fBuf), StandardCharsets.UTF_8);
    Assert.assertEquals("\"first\"\"second\"\"third\"", result);
  }

}
