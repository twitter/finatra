package com.twitter.finatra.http.integration.tweetexample.main.controllers

import com.twitter.concurrent.exp.AsyncStream
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.Controller
import com.twitter.finatra.http.integration.tweetexample.main.domain.Tweet
import com.twitter.finatra.http.integration.tweetexample.main.services.TweetsRepository
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

  get("/tweets/streaming_json") { request: Request =>
    tweetsRepository.getByIds(
      AsyncStream(0, 1, 2, 3, 4, 5))
  }

  get("/tweets/streaming_custom_tobuf") { request: Request =>
    StreamingResponse(Buf.Utf8.apply) {
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

  get("/tweets/:id") { request: Request =>
    val id = request.params("id").toLong
    tweetsRepository.getById(id)
  }
}
