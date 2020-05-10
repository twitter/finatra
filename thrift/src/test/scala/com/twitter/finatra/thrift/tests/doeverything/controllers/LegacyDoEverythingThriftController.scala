package com.twitter.finatra.thrift.tests.doeverything.controllers

import com.twitter.conversions.DurationOps._
import com.twitter.doeverything.thriftscala.{Answer, DoEverything, DoEverythingException}
import com.twitter.doeverything.thriftscala.DoEverything.{
  Ask,
  Echo,
  Echo2,
  MagicNum,
  MoreThanTwentyTwoArgs,
  Uppercase
}
import com.twitter.finagle.{ChannelException, RequestException, RequestTimeoutException}
import com.twitter.finatra.thrift.Controller
import com.twitter.finatra.thrift.tests.doeverything.exceptions.{BarException, FooException}
import com.twitter.inject.annotations.Flag
import com.twitter.inject.logging.FinagleMDCAdapter
import com.twitter.util.Future
import javax.inject.{Inject, Singleton}
import org.slf4j.MDC
import scala.collection.JavaConverters._
import scala.util.control.NoStackTrace

@Singleton
@deprecated(
  "These tests exist to ensure legacy functionaly still operates. Do not use them for guidance",
  "2018-12-20")
class LegacyDoEverythingThriftController @Inject() (@Flag("magicNum") magicNumValue: String)
    extends Controller
    with DoEverything.BaseServiceIface {

  private[this] var storedMDC: Option[Map[String, String]] = None

  override val uppercase = handle(Uppercase) { args: Uppercase.Args =>
    storeForTesting()
    info("In uppercase method.")
    if (args.msg == "fail") {
      Future.exception(new Exception("oops") with NoStackTrace)
    } else {
      Future.value(args.msg.toUpperCase)
    }
  }

  override val echo = handle(Echo) { args: Echo.Args =>
    if (args.msg == "clientError") {
      Future.exception(new Exception("client error"))
    } else {
      Future.value(args.msg)
    }
  }

  override val echo2 = handle(Echo2) { args: Echo2.Args =>
    args.msg match {
      case "clientError" => throw new Exception("client error")
      case "unknownClientIdError" => throw new Exception("unknown client id error")
      case "requestException" => throw new RequestException
      case "timeoutException" => throw new RequestTimeoutException(1.second, "timeout exception")
      case "unhandledException" => throw new Exception("unhandled exception") with NoStackTrace
      // should be handled by BarExceptionMapper and FooExceptionMapper
      case "barException" => throw new BarException
      case "fooException" => throw new FooException
      case "unhandledSourcedException" => throw new ChannelException with NoStackTrace
      // should be handled by root mapper, ThrowableExceptionMapper
      case "unhandledThrowable" => throw new Throwable("unhandled throwable")
      case _ => Future.value("no specified exception")
    }
  }

  override val magicNum = handle(MagicNum) { args: MagicNum.Args =>
    Future.value(magicNumValue)
  }

  override val moreThanTwentyTwoArgs = handle(MoreThanTwentyTwoArgs) {
    args: MoreThanTwentyTwoArgs.Args =>
      Future.value("handled")
  }

  override val ask = handle(Ask) { args: Ask.Args =>
    val question = args.question
    if (question.text.equals("fail")) {
      Future.exception(new DoEverythingException("This is a test."))
    } else {
      Future.value(Answer(s"The answer to the question: `${question.text}` is 42."))
    }
  }

  def getStoredMDC: Option[Map[String, String]] = this.storedMDC

  private def storeForTesting(): Unit = {
    this.storedMDC = Some(
      MDC.getMDCAdapter
        .asInstanceOf[FinagleMDCAdapter]
        .getPropertyContextMap
        .asScala
        .toMap
    )
  }
}
