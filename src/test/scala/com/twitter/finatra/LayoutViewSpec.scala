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


class LayoutViewSpec extends ShouldSpec {
  "A LayoutView" should "render" in {

    val posts   = List(new Post("One"), new Post("Two"))
    val layout  = new PostsView(posts)

    layout.render should include ("Posts")
    layout.render should include ("Title: One")
  }
}
