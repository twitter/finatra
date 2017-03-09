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

  test("success") {
    Await.result(client123.uppercase("Hi")) should equal("HI")
  }

  test("failure") {
    val e = assertFailedFuture[Exception] {
      client123.uppercase("fail")
    }
    e.getMessage should include("oops")
  }

  test("magicNum") {
    Await.result(client123.magicNum()) should equal("57")
  }

  test("blacklist") {
    val notWhitelistClient = server.thriftClient[DoEverything[Future]](clientId = "not_on_whitelist")
    assertFailedFuture[UnknownClientIdError] {
      notWhitelistClient.echo("Hi")
    }
  }

  test("no client id") {
    val noClientIdClient = server.thriftClient[DoEverything[Future]]()
    assertFailedFuture[NoClientIdError] {
      noClientIdClient.echo("Hi")
    }
  }

  test("more than 22 args") {
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
