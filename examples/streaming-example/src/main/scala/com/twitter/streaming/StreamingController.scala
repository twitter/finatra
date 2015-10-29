package com.twitter.streaming

import com.twitter.concurrent.exp.AsyncStream
import com.twitter.finatra.http.Controller

class StreamingController extends Controller {

  post("/tweets") { tweets: AsyncStream[Tweet] =>
    tweets map { tweet =>
      "Created tweet " + tweet
    }
  }
}
