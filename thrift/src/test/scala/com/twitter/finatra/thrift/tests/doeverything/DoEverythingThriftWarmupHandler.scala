package com.twitter.finatra.thrift.tests.doeverything

import com.twitter.doeverything.thriftscala.DoEverything.{Echo, Uppercase}
import com.twitter.finagle.thrift.ClientId
import com.twitter.finatra.thrift.routing.ThriftWarmup
import com.twitter.inject.Logging
import com.twitter.inject.utils.Handler
import javax.inject.{Inject, Singleton}
import scala.reflect.ClassTag

@Singleton
class DoEverythingThriftWarmupHandler @Inject()(
  warmup: ThriftWarmup)
  extends Handler
  with Logging {

  private val clientId = ClientId("client123")

  override def handle(): Unit = {
    try {
      clientId.asCurrent {
        // warm up thrift service(s) here
        warmup.send(
          method = Echo,
          args = Echo.Args("hello")){ result =>
            assert(result.success.isDefined)
            result.success.foreach(value => assertExpected(value, "hello"))
          }

        warmup.send(
          method = Uppercase,
          args = Uppercase.Args("hi")){ result =>
            result.success.foreach(value => assertExpected(value, "HI"))
          }
      }
    } catch {
      case e: Throwable =>
        error(e.getMessage, e)
    } finally{
      warmup.close()
    }
    info("Warm up done.")
  }

  /* Private */

  private def assertExpected[T: ClassTag](expected: T, received: T): Unit = {
    assert(expected == received,
      s"Warmup request assertion failed. Expected $expected, received: $received")
  }
}
