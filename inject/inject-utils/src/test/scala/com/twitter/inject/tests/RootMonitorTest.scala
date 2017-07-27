package com.twitter.inject.tests

import com.twitter.conversions.time._
import com.twitter.finagle.{
  BackupRequestLost,
  Failure,
  IndividualRequestTimeoutException,
  CancelledRequestException
}
import com.twitter.inject.{RootMonitor, Test}

class RootMonitorTest extends Test {

  test("RootMonitor#handle CancelledRequestException") {
    RootMonitor.handle(new CancelledRequestException) should be(true)
  }

  test("RootMonitor#handle com.twitter.util.TimeoutException") {
    RootMonitor.handle(new com.twitter.util.TimeoutException("TIMEOUT")) should be(true)
  }

  test("RootMonitor#handle com.twitter.finagle.TimeoutException") {
    RootMonitor.handle(new IndividualRequestTimeoutException(1.second)) should be(true)
  }

  test("RootMonitor#handle com.twitter.finagle.mux.ServerApplicationError") {
    RootMonitor.handle(new com.twitter.finagle.mux.ServerApplicationError("WHAT")) should be(true)
  }

  test("RootMonitor#handle restartable Failure") {
    RootMonitor.handle(Failure("Restartable", new Exception("EXCEPTION"), 1L)) should be(true)
  }

  test("RootMonitor#handle BackupRequestLost") {
    RootMonitor.handle(BackupRequestLost) should be(true)
  }

  test("RootMonitor#handle NonFatal") {
    RootMonitor.handle(new RuntimeException("NON FATAL EXCEPTION")) should be(true)
  }

  test("RootMonitor#handle Fatal") {
    // NOTE: this causes a scary looking stacktrace) the logs which is just the util/RootMonitor logging that it is
    // handling a Fatal exception.
    // see: https://github.com/twitter/util/blob/develop/util-core/src/main/scala/com/twitter/util/Monitor.scala#L191
    RootMonitor.handle(new InterruptedException()) should be(false)
  }
}
