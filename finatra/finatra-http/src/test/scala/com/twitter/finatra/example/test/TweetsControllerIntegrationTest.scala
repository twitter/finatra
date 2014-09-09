package com.twitter.finatra.example.test

import com.twitter.finagle.http.Status
import com.twitter.finatra.example.main.TweetsEndpointServer
import com.twitter.finatra.test.{EmbeddedTwitterServer, Test}
import com.twitter.util.{Await, Future, FuturePool}

class TweetsControllerIntegrationTest extends Test {

  lazy val server = EmbeddedTwitterServer(new TweetsEndpointServer)

  "get tweet 20" in {
    val tweet = server.httpGetJson[Map[String, Long]](
      "/tweets/20", andExpect = Status.Ok)

    tweet("idonly") should equal(20) //confirm response was transformed by registered TweetMessageBodyWriter
  }

  "get hello" in {
    for (i <- 1 to 1000) yield {
      getHello()
    }
  }

  val pool = FuturePool.unboundedPool
  "get hello in parallel" in {
    Await.result {
      Future.collect {
        for (i <- 1 to 1000) yield {
          pool {
            getHello()
          }
        }
      }
    }
  }

  def getHello() = {
    server.httpGet(
      "/tweets/hello", andExpect = Status.Ok, withBody = "hello world", suppress = true)
  }
}
