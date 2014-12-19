/**
 * Copyright (C) 2012 Twitter Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.twitter.finatra

import com.twitter.finatra.config.logLevel
import test.ShouldSpec
import com.twitter.app.App
import com.twitter.logging.Level

class LoggingSpec extends ShouldSpec {

  def changeLevel(level: String)(f: => Unit) = {
    logLevel.let(level) {
      f
    }
  }

  trait TestApp extends App with Logging

  "logLevel" should "be INFO by default" in {
    new TestApp {
      logLevel should equal(Some(Level.INFO))
    }
  }

  "flag settings" should "work" in {
    changeLevel("DEBUG") {
      config.logLevel() should equal("DEBUG")
    }
  }

  "logLevel" should "respect flag settings" in {
    new TestApp {
      changeLevel("DEBUG") {
        logLevel should equal(Some(Level.DEBUG))
      }
    }
  }

  "logLevel" should "throw on a wrong level name" in {
    changeLevel("Blah") {
      an [IllegalArgumentException] should be thrownBy {
        new TestApp {
          val foo = logLevel
        }
      }
    }
  }

}
