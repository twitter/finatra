package com.twitter.finatra.http.tests.streaming;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import scala.collection.JavaConverters;
import scala.reflect.Manifest;
import scala.reflect.ManifestFactory;
import scala.runtime.BoxedUnit;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.twitter.concurrent.AsyncStream;
import com.twitter.finagle.http.Response;
import com.twitter.finagle.stats.StatsReceiver;
import com.twitter.finatra.http.marshalling.MessageBodyManager;
import com.twitter.finatra.http.response.ResponseBuilder;
import com.twitter.finatra.http.streaming.StreamingResponse;
import com.twitter.finatra.http.streaming.ToReader;
import com.twitter.finatra.json.FinatraObjectMapper;
import com.twitter.finatra.utils.FileResolver;

import com.twitter.io.Buf;
import com.twitter.io.BufReaders;
import com.twitter.io.Bufs;
import com.twitter.io.Reader;
import com.twitter.io.Readers;
import com.twitter.io.StreamTermination;
import com.twitter.util.Await;
import com.twitter.util.Duration;
import com.twitter.util.Function;
import com.twitter.util.Future;

public class StreamingResponseJavaTest extends Assert {

  private Future<BoxedUnit> burnLoop(Reader<Buf> reader) {
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

  private <A> A await(Future<A> awaitable) throws Exception {
    return Await.result(awaitable, Duration.apply(5, TimeUnit.SECONDS));
  }

  @SuppressWarnings("rawtypes")
  private <A> Response fromReader(Reader<A> reader, Manifest<A> manifest) throws Exception {
    StreamingResponse<Reader, A> streamingResponse =
        responseBuilder.streaming(reader, ToReader.ReaderIdentity(), manifest);
    Future<Response> fResponse = streamingResponse.toFutureResponse();
    return await(fResponse);
  }

  private Reader<Buf> infiniteReader(Buf buf) {
    Stream<Buf> infiniteStream = Stream.iterate(buf, i -> i.concat(buf));
    return Readers.fromSeq(infiniteStream);
  }

  @Test
  public void emptyReader() throws Exception {
    Reader<Object> reader = Readers.newEmptyReader();
    Response response = fromReader(reader, ManifestFactory.Object());
    Future<Buf> fBuf = BufReaders.readAll(response.reader());
    String result = Buf.decodeString(await(fBuf), StandardCharsets.UTF_8);
    Assert.assertEquals("[]", result);
    Assert.assertEquals(
        await(response.reader().onClose()), StreamTermination.FullyRead$.MODULE$);
  }

  @Test
  public void serdeReaderofString() throws Exception {
    List<String> stringList = Arrays.asList("first", "second", "third");
    Reader<String> reader = Readers.fromSeq(stringList);
    Response response = fromReader(reader, ManifestFactory.<String>classType(String.class));
    Future<Buf> fBuf = BufReaders.readAll(response.reader());
    String result = Buf.decodeString(await(fBuf), StandardCharsets.UTF_8);
    Assert.assertEquals("firstsecondthird", result);
  }

  @Test
  public void serdeReaderofObject() throws Exception {
    FooClass f1 = new FooClass(1, "first");
    FooClass f2 = new FooClass(2, "second");
    List<FooClass> fooList = Arrays.asList(f1, f2);
    Reader<FooClass> reader = Readers.fromSeq(fooList);
    Response response = fromReader(reader, ManifestFactory.<FooClass>classType(FooClass.class));
    Future<Buf> fBuf = BufReaders.readAll(response.reader());
    String result = Buf.decodeString(await(fBuf), StandardCharsets.UTF_8);
    Assert.assertEquals("[{\"v1\":1,\"v2\":\"first\"},{\"v1\":2,\"v2\":\"second\"}]", result);
  }

  @Test
  public void readerWriteFailureWithReaderDicardedException() throws Exception {
    Reader<Buf> reader = infiniteReader(Bufs.UTF_8.apply("foo"));
    Response response = fromReader(reader, ManifestFactory.<Buf>classType(Buf.class));
    response.reader().discard();
    burnLoop(response.reader());
    Assert.assertEquals(
        await(response.reader().onClose()), StreamTermination.Discarded$.MODULE$);
  }

  @SuppressWarnings("rawtypes")
  private <A> Response fromStream(AsyncStream<A> stream, Manifest<A> manifest) throws Exception {
    StreamingResponse<AsyncStream, A> streamingResponse = responseBuilder.streaming(
      stream, ToReader.AsyncStreamToReader(), manifest);
    Future<Response> fResponse = streamingResponse.toFutureResponse();
    return await(fResponse);
  }

  @Test
  public void serdeAsyncStreamOfString() throws Exception {
    List<String> stringList = Arrays.asList("first", "second", "third");
    AsyncStream<String> stream = AsyncStream.fromSeq(
      JavaConverters.asScalaIteratorConverter(stringList.iterator()).asScala().toSeq());
    Response response = fromStream(stream, ManifestFactory.<String>classType(String.class));
    Future<Buf> fBuf = BufReaders.readAll(response.reader());
    String result = Buf.decodeString(await(fBuf), StandardCharsets.UTF_8);
    Assert.assertEquals("firstsecondthird", result);
  }

}
