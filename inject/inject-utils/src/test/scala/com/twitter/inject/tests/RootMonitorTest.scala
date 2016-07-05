package com.twitter.inject.tests

import com.twitter.conversions.time._
import com.twitter.finagle.{BackupRequestLost, Failure, IndividualRequestTimeoutException, CancelledRequestException}
import com.twitter.inject.{RootMonitor, Test}

class RootMonitorTest extends Test {

  "RootMonitor" should {

    "handle CancelledRequestException" in {
      RootMonitor.handle(new CancelledRequestException) should be(true)
    }

    "handle com.twitter.util.TimeoutException" in {
      RootMonitor.handle(new com.twitter.util.TimeoutException("TIMEOUT")) should be(true)
    }

    "handle com.twitter.finagle.TimeoutException" in {
      RootMonitor.handle(new IndividualRequestTimeoutException(1.second)) should be(true)
    }

    "handle com.twitter.finagle.mux.ServerApplicationError" in {
      RootMonitor.handle(new com.twitter.finagle.mux.ServerApplicationError("WHAT")) should be(true)
    }

    "handle restartable Failure" in {
      RootMonitor.handle(Failure("Restartable", new Exception("EXCEPTION"), 1L)) should be(true)
    }

    "handle BackupRequestLost" in {
      RootMonitor.handle(BackupRequestLost) should be(true)
    }

    "handle NonFatal" in {
      RootMonitor.handle(new RuntimeException("NON FATAL EXCEPTION")) should be(true)
    }

    "handle Fatal" in {
      // NOTE: this causes a scary looking stacktrace in the logs which is just the util/RootMonitor logging that it is
      // handling a Fatal exception.
      // see: https://github.com/twitter/util/blob/develop/util-core/src/main/scala/com/twitter/util/Monitor.scala#L191
      RootMonitor.handle(new InterruptedException()) should be(false)
    }
  }

}
