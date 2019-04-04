package com.twitter.streaming

import com.twitter.finatra.http.Controller
import com.twitter.io.Reader

class StreamingController extends Controller {

  post("/tweets") { tweets: Reader[Tweet] =>
    tweets map { tweet =>
      "Created tweet " + tweet
    }
  }
}
