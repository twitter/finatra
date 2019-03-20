package com.twitter.finatra.thrift.tests

import com.twitter.doeverything.thriftscala.DoEverything.Uppercase
import com.twitter.finatra.thrift.routing.{ThriftWarmup, ThriftRouter}
import com.twitter.inject.Test

class ThriftWarmupTest extends Test {
  test("ThriftWarmup refuses to send a request if a router is not configured") {
    intercept[IllegalStateException] {
      val tw = new ThriftWarmup(new ThriftRouter(null, null, null))
      tw.send(Uppercase, Uppercase.Args("hithere"), 1)()
    }
  }
}
