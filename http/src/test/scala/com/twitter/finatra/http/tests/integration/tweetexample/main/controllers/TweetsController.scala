package com.twitter.finatra.http.tests.integration.tweetexample.main.controllers

import com.twitter.concurrent.AsyncStream
import com.twitter.finagle.http.{Fields, Request, Response, Status}
import com.twitter.finatra.http.Controller
import com.twitter.finatra.http.response.{
  StreamingResponseUtils,
  StreamingResponse => DeprecatedStreamingResponse
}
import com.twitter.finatra.http.streaming.StreamingRequest
import com.twitter.finatra.http.tests.integration.tweetexample.main.domain.Tweet
import com.twitter.finatra.http.tests.integration.tweetexample.main.services.TweetsRepository
import com.twitter.io.{Buf, Reader}
import com.twitter.util.{Duration, Future, Try}
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import scala.collection.mutable

class TweetsController @Inject()(
  tweetsRepository: TweetsRepository,
  onWriteLog: mutable.ArrayBuffer[String])
    extends Controller {

  get("/tweets/hello") { request: Request =>
    "hello world"
  }

  post("/tweets/") { tweet: Tweet =>
    "tweet with id " + tweet.id + " is valid"
  }

  post("/tweets/streaming") { ids: Reader[Long] =>
    tweetsRepository.getByIds(ids)
  }

  post("/tweets/reader_buf_to_string") { bufs: Reader[Buf] =>
    bufs.map(buf => Buf.decodeString(buf, StandardCharsets.UTF_8))
  }

  post("/tweets/reader_buf") { bufs: Reader[Buf] =>
    bufs
  }

  post("/tweets/asyncStream_buf_to_string") { bufs: AsyncStream[Buf] =>
    bufs.map(buf => Buf.decodeString(buf, StandardCharsets.UTF_8))
  }

  post("/tweets/asyncStream_buf") { bufs: AsyncStream[Buf] =>
    bufs
  }

  get("/tweets/streaming_json") { request: Request =>
    tweetsRepository.getByIds(Reader.fromSeq(Seq(0, 1, 2, 3, 4, 5)))
  }

  get("/tweets/streaming_custom_tobuf") { request: Request =>
    DeprecatedStreamingResponse(Buf.Utf8.apply) {
      AsyncStream("A", "B", "C")
    }
  }

  get("/tweets/streamingRep_with_asyncStream") { _: Request =>
    response.streaming(AsyncStream("A", "B", "C"))
  }

  get("/tweets/streamingRep_with_reader") { _: Request =>
    response.streaming(Reader.fromSeq(Seq(1, 2, 3)))
  }

  get("/tweets/streaming_with_transformer") { _: Request =>
    def lowercaseTransformer(as: AsyncStream[String]) = as.map(_.toLowerCase)

    val transformer = (lowercaseTransformer _)
      .andThen(StreamingResponseUtils.toBufTransformer(Buf.Utf8.apply) _)
      .andThen(StreamingResponseUtils.tupleTransformer(()) _)

    def onWrite(ignored: Unit, buf: Buf)(t: Try[Unit]): Unit = ()

    DeprecatedStreamingResponse(
      transformer,
      Status.Ok,
      Map.empty,
      onWrite,
      () => (),
      Duration.Zero
    ) {
      AsyncStream("A", "B", "C")
    }
  }

  get("/tweets/streamingRep_with_transformer_asyncStream") { _: Request =>
    response.streaming(AsyncStream("A", "B", "C").map(_.toLowerCase))
  }

  get("/tweets/streamingRep_with_transformer_reader") { _: Request =>
    response.streaming(Reader.fromSeq(Seq("A", "B", "C")).map(_.toLowerCase))
  }

  get("/tweets/streaming_with_onWrite") { _: Request =>
    def serializeAndLowercase(as: AsyncStream[String]): AsyncStream[(String, Buf)] = {
      as.map(str => (str.toLowerCase, Buf.Utf8(str)))
    }

    def onWrite(lowerCased: String, buf: Buf)(t: Try[Unit]): Unit = {
      onWriteLog.append(lowerCased)
    }

    DeprecatedStreamingResponse(
      serializeAndLowercase,
      Status.Ok,
      Map.empty,
      onWrite,
      () => (),
      Duration.Zero
    ) {
      AsyncStream("A", "B", "C")
    }
  }

  get("/tweets/streaming_custom_tobuf_with_custom_headers") { request: Request =>
    val headers = Map(
      Fields.ContentType -> "text/event-stream;charset=UTF-8",
      Fields.CacheControl -> "no-cache, no-store, max-age=0, must-revalidate",
      Fields.Pragma -> "no-cache"
    )

    DeprecatedStreamingResponse(Buf.Utf8.apply, Status.Created, headers) {
      AsyncStream("A", "B", "C")
    }
  }

  get("/tweets/streaming_manual_writes") { request: Request =>
    val response = Response()
    response.setChunked(true)

    response.writer.write(Buf.Utf8("hello")) before {
      response.writer.write(Buf.Utf8("world")) ensure {
        response.close()
      }
    }

    Future(response)
  }

  post("/tweets/streaming_with_streamingRequest") {
    streamingRequest: StreamingRequest[Reader, Long] =>
      val reader = streamingRequest.stream
      tweetsRepository.getByIds(reader)
  }

  get("/tweets/not_streaming_with_streamingRequest/:id") {
    streamingRequest: StreamingRequest[Reader, Long] =>
      val id = streamingRequest.request.params("id").toLong
      tweetsRepository.getById(id)
  }

  get("/tweets/") { request: Request =>
    "tweets root"
  }

  get("/tweets/:id") { request: Request =>
    val id = request.params("id").toLong
    tweetsRepository.getById(id)
  }

  get("/tweets/test/:id/") { request: Request =>
    request.params("id")
  }
}
