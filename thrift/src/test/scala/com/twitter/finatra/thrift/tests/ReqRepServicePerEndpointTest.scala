package com.twitter.finatra.thrift.tests

import com.twitter.conversions.time._
import com.twitter.finagle.Filter.TypeAgnostic
import com.twitter.finagle.thrift.ClientId
import com.twitter.finagle.{Filter, Service, mux}
import com.twitter.inject.Test
import com.twitter.io.Buf
import com.twitter.scrooge.HeaderMap
import com.twitter.util.{Await, Future}
import java.nio.charset.{StandardCharsets => JChar}

object ReqRepServicePerEndpointTest {
  /* filter out com.twitter.finagle header keys */
  def filteredHeaders(headers: HeaderMap): Map[String, Seq[Buf]] = {
    headers.toMap.filterKeys(key => !key.startsWith("com.twitter.finagle"))
  }

  def printHeaders(headers: Map[String, Seq[Buf]]): String = {
    (for {
      (key, values) <- headers.mapValues(values => values.map(Buf.Utf8.unapply(_).get))
    } yield {
      s"$key -> ${values.mkString(", ")}"
    }).mkString(", ")
  }

  def loggingFilter(fn: => Unit) = new TypeAgnostic {
    def toFilter[Req, Rep]: Filter[Req, Rep, Req, Rep] = {
      new Filter[Req, Rep, Req, Rep] {
        def apply(
          request: Req,
          service: Service[Req, Rep]
        ): Future[Rep] = {
          service(request).ensure(fn)
        }
      }
    }
  }
}

abstract class ReqRepServicePerEndpointTest extends Test {
  protected val TestClientId: ClientId = ClientId("client123")
  protected val TestClientRequestHeaderKey = "com.twitter.client123.header"

  protected class MuxContextsFilter {
    private var _contexts: Seq[(Buf, Buf)] = Seq.empty

    def toFilter: Filter[mux.Request, mux.Response, mux.Request, mux.Response] = new Filter[mux.Request, mux.Response, mux.Request, mux.Response] {
      def apply(
        request: mux.Request,
        service: Service[mux.Request, mux.Response]
      ): Future[mux.Response] = {
        for (response <- service(request)) yield {
          _contexts = response.contexts
          response
        }
      }
    }

    def contexts: Seq[(Buf, Buf)] = _contexts

    def clear():Unit = synchronized {
      _contexts = Seq.empty
    }
  }

  protected def muxContextsFilter = new MuxContextsFilter

  protected val specificMethodLoggingFilter = new TypeAgnostic {
    def toFilter[Req, Rep]: Filter[Req, Rep, Req, Rep] = {
      new Filter[Req, Rep, Req, Rep] {
        def apply(
          request: Req,
          service: Service[Req, Rep]
        ): Future[Rep] = {
          info("METHOD CLIENT-SIDE FILTER")
          service(request)
        }
      }
    }
  }

  protected val globalLoggingFilter = new TypeAgnostic {
    def toFilter[Req, Rep]: Filter[Req, Rep, Req, Rep] = {
      new Filter[Req, Rep, Req, Rep] {
        def apply(
          request: Req,
          service: Service[Req, Rep]
        ): Future[Rep] = {
          info("GLOBAL CLIENT-SIDE FILTER")
          service(request)
        }
      }
    }
  }

  protected def await[T](f: Future[T]): T = {
    Await.result(f, 5.seconds)
  }

  /** assumes UTF-8 encoding */
  protected def decodeBuf(buf: Buf): String = {
    Buf.decodeString(buf, JChar.UTF_8)
  }
}
