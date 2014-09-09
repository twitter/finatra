package com.twitter.finatra.logging

import com.twitter.finatra.test.Test

class TimingTest extends Test {

  "timing" in {
    new Foo().stuff()
  }

  class Foo extends Timing {
    def stuff() {
      time("time %s ms") {
        1 + 2
      }
    }
  }

}
