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
package com.twitter.finatra.test

import com.twitter.finatra.{Controller, FinatraServer}
import com.twitter.logging.Logger


class TestApp extends Controller {

  get("/hey") {
    request =>  render.plain("hello").toFuture
  }

}

class FinatraServerSpec extends SpecHelper {

  def app = { new TestApp }

//  "app" should "register" in {
//    FinatraServer.register(app)
//  }

}
