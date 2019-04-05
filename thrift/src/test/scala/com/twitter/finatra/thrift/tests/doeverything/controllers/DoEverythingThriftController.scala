package com.twitter.finatra.thrift.tests.doeverything.controllers

import com.twitter.conversions.DurationOps._
import com.twitter.doeverything.thriftscala.{Answer, DoEverything, DoEverythingException}
import com.twitter.doeverything.thriftscala.DoEverything.{Ask, Echo, Echo2, MagicNum, MoreThanTwentyTwoArgs, Uppercase}
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.{ChannelException, Filter, RequestException, RequestTimeoutException, Service, SimpleFilter}
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
class DoEverythingThriftController @Inject()(@Flag("magicNum") magicNumValue: String, stats: StatsReceiver)
    extends Controller(DoEverything) {

  private[this] val echos = stats.counter("echo_calls")

  private[this] val countEchoFilter = new Filter.TypeAgnostic {
    def toFilter[Req, Rep]: Filter[Req, Rep, Req, Rep] = new SimpleFilter[Req, Rep]{
      def apply(request: Req, service: Service[Req, Rep]): Future[Rep] = {
        echos.incr()
        service(request)
      }
    }
  }

  private[this] var storedMDC: Option[Map[String, String]] = None

  handle(Uppercase) { args: Uppercase.Args =>
    storeForTesting()
    info("In uppercase method.")
    if (args.msg == "fail") {
      Future.exception(new Exception("oops") with NoStackTrace)
    } else {
      Future.value(args.msg.toUpperCase)
    }
  }

  handle(Echo).filtered(countEchoFilter) { args: Echo.Args =>
    if (args.msg == "clientError") {
      Future.exception(new Exception("client error") with NoStackTrace)
    } else {
      Future.value(args.msg)
    }
  }

  handle(Echo2).filtered(countEchoFilter) { args: Echo2.Args =>
    args.msg match {
      case "clientError" => throw new Exception("client error")
      case "unknownClientIdError" => throw new Exception("unknown client id error")
      case "requestException" => throw new RequestException
      case "timeoutException" => throw new RequestTimeoutException(1.second, "timeout exception")
      case "unhandledException" => throw new Exception("unhandled exception") with NoStackTrace
      // should be handled by ReqRepBarExceptionMapper and ReqRepFooExceptionMapper
      case "barException" => throw new BarException
      case "fooException" => throw new FooException
      case "unhandledSourcedException" => throw new ChannelException with NoStackTrace
      // should be handled by root mapper, ThrowableExceptionMapper
      case "unhandledThrowable" => throw new Throwable("unhandled throwable")
      case _ => Future.value("no specified exception")
    }
  }

  handle(MagicNum) { args: MagicNum.Args =>
    Future.value(magicNumValue)
  }

  handle(MoreThanTwentyTwoArgs) {
    args: MoreThanTwentyTwoArgs.Args =>
      Future.value("handled")
  }

  handle(Ask) { args: Ask.Args =>
    val question = args.question
    if (question.text.equals("fail")) {
      Future.exception(new DoEverythingException("This is a test."))
    } else {
      Future.value(
        Answer(s"The answer to the question: `${question.text}` is 42."))
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
