package com.twitter.finatra.http.tests.integration.tweetexample.main.controllers

import com.twitter.concurrent.AsyncStream
import com.twitter.finagle.http.{Fields, Status, Request, Response}
import com.twitter.finatra.http.Controller
import com.twitter.finatra.http.tests.integration.tweetexample.main.domain.Tweet
import com.twitter.finatra.http.tests.integration.tweetexample.main.services.TweetsRepository
import com.twitter.finatra.http.response.StreamingResponse
import com.twitter.io.Buf
import com.twitter.util.Future
import javax.inject.Inject

class TweetsController @Inject()(
  tweetsRepository: TweetsRepository)
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
    tweetsRepository.getByIds(
      AsyncStream(0, 1, 2, 3, 4, 5))
  }

  get("/tweets/streaming_custom_tobuf") { request: Request =>
    StreamingResponse(Buf.Utf8.apply) {
      AsyncStream("A", "B", "C")
    }
  }

  get("/tweets/streaming_custom_tobuf_with_custom_headers") { request: Request =>
    val headers = Map(
      Fields.ContentType -> "text/event-stream;charset=UTF-8",
      Fields.CacheControl -> "no-cache, no-store, max-age=0, must-revalidate",
      Fields.Pragma -> "no-cache")

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
