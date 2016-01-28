package com.twitter.finatra.thrift.tests

import com.twitter.doeverything.thriftscala.DoEverything
import com.twitter.finatra.thrift.EmbeddedThriftServer
import com.twitter.finatra.thrift.tests.doeverything.DoEverythingThriftServer
import com.twitter.finatra.thrift.thriftscala.{NoClientIdError, UnknownClientIdError}
import com.twitter.inject.server.FeatureTest
import com.twitter.util.{Await, Future}

class DoEverythingThriftServerFeatureTest extends FeatureTest {
  override val server = new EmbeddedThriftServer(
    twitterServer = new DoEverythingThriftServer,
    flags = Map("magicNum" -> "57"))

  val client123 = server.thriftClient[DoEverything[Future]](clientId = "client123")

  "success" in {
    Await.result(client123.uppercase("Hi")) should equal("HI")
  }

  "failure" in {
    val e = assertFailedFuture[Exception] {
      client123.uppercase("fail")
    }
    e.getMessage should include("oops")
  }

  "magicNum" in {
    Await.result(client123.magicNum()) should equal("57")
  }

  "blacklist" in {
    val notWhitelistClient = server.thriftClient[DoEverything[Future]](clientId = "not_on_whitelist")
    assertFailedFuture[UnknownClientIdError] {
      notWhitelistClient.echo("Hi")
    }
  }

  "no client id" in {
    val noClientIdClient = server.thriftClient[DoEverything[Future]]()
    assertFailedFuture[NoClientIdError] {
      noClientIdClient.echo("Hi")
    }
  }

  "more than 22 args" in {
    Await.result(
      client123.moreThanTwentyTwoArgs(
        "one",
        "two",
        "three",
        "four",
        "five",
        "six",
        "seven",
        "eight",
        "nine",
        "ten",
        "eleven",
        "twelve",
        "thirteen",
        "fourteen",
        "fifteen",
        "sixteen",
        "seventeen",
        "eighteen",
        "nineteen",
        "twenty",
        "twentyone",
        "twentytwo",
        "twentythree")
    ) should equal("handled")
  }
}
