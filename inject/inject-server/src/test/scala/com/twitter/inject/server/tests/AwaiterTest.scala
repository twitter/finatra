package com.twitter.inject.server.tests

import com.twitter.conversions.DurationOps._
import com.twitter.inject.server.Awaiter
import com.twitter.inject.{PoolUtils, Test}
import com.twitter.util.{Await, Awaitable, ExecutorServiceFuturePool, Future, Promise}

class AwaiterTest extends Test {
  private[this] val futurePool: ExecutorServiceFuturePool =
    PoolUtils.newFixedPool(this.getClass.getName)

  test("Empty list") {
    // should not error or hang indefinitely
    Awaiter.any(Seq.empty[Awaitable[_]], period = 1.second)
  }

  test("Awaiting") {
    val p = new Promise[Unit]
    var n = 0
    val f = futurePool {
      Awaiter.any(Seq(p), period = 500.millis)
      n = n + 1
    }

    f.isDefined should be(false)
    p.setDone
    Await.ready(f, 2.seconds)

    f.isDefined should be(true)
    n should equal(1)
  }

  test("Await any exit") {
    val p1 = Future.never
    val p2 = new Promise[Unit]

    var n = 0
    val f = futurePool {
      Awaiter.any(Seq(p1, p2), period = 500.millis)
      n = n + 1
    }

    f.isDefined should be(false)
    p2.setDone // should force Await.any to exit
    Await.ready(f, 2.seconds)

    f.isDefined should be(true)
    n should equal(1)
  }
}
