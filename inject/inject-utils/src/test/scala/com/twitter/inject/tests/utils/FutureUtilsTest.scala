package com.twitter.inject.tests.utils

import com.twitter.inject.Test
import com.twitter.inject.utils.FutureUtils
import com.twitter.util.{Await, Future, FuturePool}
import java.util.concurrent.{ConcurrentLinkedQueue, Executors}
import scala.collection.JavaConverters._

class FutureUtilsTest extends Test {
  val ActionLog = new ConcurrentLinkedQueue[String]
  val TestFuturePool = FuturePool(Executors.newFixedThreadPool(4))

  override def beforeEach {
    ActionLog.clear()
  }
  
  test("FutureUtils#sequentialMap") {
    assertFuture(
      FutureUtils.sequentialMap(Seq(1, 2, 3))(mockSvcCall),
      Future(Seq("1", "2", "3")))

    ActionLog.asScala.toSeq should equal(Seq("S1", "E1", "S2", "E2", "S3", "E3"))
  }

  test("FutureUtils#collectMap") {
    val result = Await.result(
      FutureUtils.collectMap(Seq(1, 2, 3))(mockSvcCall))
    result.sorted should equal(Seq("1", "2", "3"))

    ActionLog.asScala.toSeq.sorted should equal(Seq("S1", "E1", "S2", "E2", "S3", "E3").sorted)
  }

  test("FutureUtils#success future") {
    val f = Await.result(FutureUtils.exceptionsToFailedFuture {
      Future("hi")
    })
    f should equal("hi")
  }

  test("FutureUtils#failed future") {
    val e = intercept[Exception] {
      Await.result(FutureUtils.exceptionsToFailedFuture {
        Future.exception(new Exception("failure"))
      })
    }
    e.getMessage should equal("failure")
  }

  test("FutureUtils#thrown exception") {
    val e = intercept[Exception] {
      Await.result(FutureUtils.exceptionsToFailedFuture {
        throw new Exception("failure")
      })
    }
    e.getMessage should equal("failure")
  }

  def mockSvcCall(num: Int): Future[String] = {
    ActionLog add ("S" + num)
    TestFuturePool {
      Thread.sleep(250)
      ActionLog add ("E" + num)
      num.toString
    }
  }
}
