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

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import com.twitter.finatra.Controller

class SampleSpec extends FlatSpec with ShouldMatchers {

  class SampleController extends Controller {
    get("/testing") {
      request => render.body("hello world").status(200).toFuture
    }
  }

  "Sample Use Case" should "allow us to instantiate separate controller for each test" in {
    val app: MockApp = MockApp(new SampleController)

    // When
    val response = app.get("/testing")

    // Then
    response.code should be(200)
    response.body should be("hello world")
  }
}