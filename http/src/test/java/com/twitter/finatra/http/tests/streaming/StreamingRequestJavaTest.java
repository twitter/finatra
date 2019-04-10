package com.twitter.finatra.http.tests.streaming;

import java.util.Arrays;
import java.util.List;

import scala.collection.JavaConverters;
import scala.reflect.ManifestFactory;

import org.junit.Assert;
import org.junit.Test;

import com.twitter.concurrent.AsyncStream;
import com.twitter.finagle.http.Method;
import com.twitter.finagle.http.Request;
import com.twitter.finagle.http.Version;
import com.twitter.finatra.http.streaming.FromReader;
import com.twitter.finatra.http.streaming.StreamingRequest;
import com.twitter.finatra.json.FinatraObjectMapper;
import com.twitter.finatra.json.internal.streaming.JsonStreamParser;
import com.twitter.io.Buf;
import com.twitter.io.Bufs;
import com.twitter.io.Reader;
import com.twitter.io.Readers;
import com.twitter.util.Await;

public class StreamingRequestJavaTest extends Assert {

  private String jsonStr = "[\"first\",\"second\",\"third\"]";

  private JsonStreamParser parser = new JsonStreamParser(FinatraObjectMapper.create(null));

  @Test
  public void requestImplicitlyToAsyncStreamOfString() throws Exception {
    List<Buf> bufList = Arrays.asList(
      Bufs.UTF_8.apply(jsonStr.substring(0, 1)),
      Bufs.UTF_8.apply(jsonStr.substring(1, 4)),
      Bufs.UTF_8.apply(jsonStr.substring(4)));

    Reader<Buf> reader = Readers.fromSeq(bufList);
    Request request = Request.apply(Version.Http11(), Method.Post(), "/", reader);
    StreamingRequest streamingRequest =
      StreamingRequest.fromRequest(
        parser,
        request,
        FromReader.AsyncStreamFromReader(),
        ManifestFactory.classType(String.class));

    AsyncStream<String> stream =
      (AsyncStream<String>) streamingRequest.stream();
    List<String> result = JavaConverters.seqAsJavaListConverter(
      Await.result(stream.toSeq())).asJava();
    List<String> expected = Arrays.asList("first", "second", "third");
    Assert.assertEquals(expected, result);
  }

  @Test
  public void requestImplicitlyToReaderOfString() throws Exception {
    List<Buf> bufList = Arrays.asList(
      Bufs.UTF_8.apply(jsonStr.substring(0, 1)),
      Bufs.UTF_8.apply(jsonStr.substring(1, 4)),
      Bufs.UTF_8.apply(jsonStr.substring(4)));

    Reader<Buf> bufReader = Readers.fromSeq(bufList);
    Request request = Request.apply(Version.Http11(), Method.Post(), "/", bufReader);
    StreamingRequest streamingRequest =
      StreamingRequest.fromRequest(
        parser,
        request,
        FromReader.ReaderIdentity(),
        ManifestFactory.classType(String.class));

    Reader<String> reader = (Reader<String>) streamingRequest.stream();
    List<String> result =
      JavaConverters.seqAsJavaListConverter(
        Await.result(Readers.toAsyncStream(reader).toSeq())).asJava();
    List<String> expected = Arrays.asList("first", "second", "third");
    Assert.assertEquals(expected, result);
  }
}
