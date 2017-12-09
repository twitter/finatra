package com.twitter.finatra.thrift.tests

import com.twitter.doeverything.thriftscala.{Answer, DoEverything, Question}
import com.twitter.finatra.thrift.EmbeddedThriftServer
import com.twitter.finatra.thrift.tests.doeverything.DoEverythingThriftServer
import com.twitter.finatra.thrift.thriftscala.{
  ClientError,
  NoClientIdError,
  ServerError,
  UnknownClientIdError
}
import com.twitter.inject.server.FeatureTest
import com.twitter.util.{Await, Future}
import org.apache.thrift.TApplicationException

class DoEverythingThriftServerFeatureTest extends FeatureTest {
  override val server = new EmbeddedThriftServer(
    twitterServer = new DoEverythingThriftServer,
    flags = Map("magicNum" -> "57")
  )

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
    val notWhitelistClient =
      server.thriftClient[DoEverything[Future]](clientId = "not_on_whitelist")
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

  // echo method doesn't define throws ClientError Exception
  // we should receive TApplicationException
  test("ClientError throw back") {
    assertFailedFuture[TApplicationException] {
      client123.echo("clientError")
    }
  }

  // should be caught by FinatraThriftExceptionMapper
  test("ThriftException#ClientError mapping") {
    val e = assertFailedFuture[ClientError] {
      client123.echo2("clientError")
    }
    e.getMessage should include("client error")
  }

  test("ThriftException#UnknownClientIdError mapping") {
    val e = assertFailedFuture[UnknownClientIdError] {
      client123.echo2("unknownClientIdError")
    }
    e.getMessage should include("unknown client id error")
  }

  test("ThriftException#RequestException mapping") {
    assertFailedFuture[ServerError] {
      client123.echo2("requestException")
    }
  }

  test("ThriftException#TimeoutException mapping") {
    assertFailedFuture[ClientError] {
      client123.echo2("timeoutException")
    }
  }

  // should be caught by BarExceptionMapper
  test("BarException mapping") {
    Await.result(client123.echo2("barException")) should equal("BarException caught")
  }
  // should be caught by FooExceptionMapper
  test("FooException mapping") {
    Await.result(client123.echo2("fooException")) should equal("FooException caught")
  }

  test("ThriftException#UnhandledSourcedException mapping") {
    assertFailedFuture[ServerError] {
      client123.echo2("unhandledSourcedException")
    }
  }

  test("ThriftException#UnhandledException mapping") {
    assertFailedFuture[ServerError] {
      client123.echo2("unhandledException")
    }
  }

  // should be caught by framework root exception mapper - ThrowableExceptionMapper
  test("ThriftException#UnhandledThrowable mapping") {
    assertFailedFuture[TApplicationException] {
      client123.echo2("unhandledThrowable")
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
        "twentythree"
      )
    ) should equal("handled")
  }

  test("ask") {
    val question = Question("What is the meaning of life?")
    Await.result(client123.ask(question)) should equal(Answer("The answer to the question: `What is the meaning of life?` is 42."))
  }

  test("ask fail") {
    val question = Question("fail")
    Await.result(client123.ask(question)) should equal(Answer("DoEverythingException caught"))
  }
}
