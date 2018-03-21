package com.twitter.finatra.http.tests.integration.tweetexample.main.controllers

import com.twitter.concurrent.AsyncStream
import com.twitter.finagle.http.{Fields, Request, Response, Status}
import com.twitter.finatra.http.Controller
import com.twitter.finatra.http.tests.integration.tweetexample.main.domain.Tweet
import com.twitter.finatra.http.tests.integration.tweetexample.main.services.TweetsRepository
import com.twitter.finatra.http.response.{StreamingResponse, StreamingResponseUtils}
import com.twitter.io.Buf
import com.twitter.util.{Duration, Future, Try}
import javax.inject.Inject

import scala.collection.mutable

class TweetsController @Inject()(
  tweetsRepository: TweetsRepository,
  onWriteLog: mutable.ArrayBuffer[String]
)
  extends Controller {

  get("/tweets/hello") { request: Request =>
    "hello world"
  }

  post("/tweets/") { tweet: Tweet =>
    "tweet with id " + tweet.id + " is valid"
  }

  post("/tweets/streaming") { ids: AsyncStream[Long] =>
    tweetsRepository.getByIds(ids)
  }

  get("/tweets/streaming_json") { request: Request =>
    tweetsRepository.getByIds(AsyncStream(0, 1, 2, 3, 4, 5))
  }

  get("/tweets/streaming_custom_tobuf") { request: Request =>
    StreamingResponse(Buf.Utf8.apply) {
      AsyncStream("A", "B", "C")
    }
  }

  get("/tweets/streaming_with_transformer") { _: Request =>
    def lowercaseTransformer(as: AsyncStream[String]) = as.map(_.toLowerCase)

    val transformer = (lowercaseTransformer _)
      .andThen(StreamingResponseUtils.toBufTransformer(Buf.Utf8.apply) _)
      .andThen(StreamingResponseUtils.tupleTransformer(()) _)

    def onWrite(ignored: Unit, buf: Buf)(t: Try[Unit]): Unit = ()

    StreamingResponse(
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

  get("/tweets/streaming_with_onWrite") { _: Request =>

    def serializeAndLowercase(as: AsyncStream[String]): AsyncStream[(String, Buf)] = {
      as.map(str => (str.toLowerCase, Buf.Utf8(str)))
    }

    def onWrite(lowerCased: String, buf: Buf)(t: Try[Unit]): Unit = {
      onWriteLog.append(lowerCased)
    }

    StreamingResponse(
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

    StreamingResponse(Buf.Utf8.apply, Status.Created, headers) {
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
