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

class ViewSpec extends ShouldSpec {
  "A view" should "render" in {

    val posts = List(new Post("One"), new Post("Two"))
    val view  = new PostsListView(posts)

    view.contentType should equal (Some("text/plain"))
    view.render should include ("Title: Two")
    view.render should include ("Title: One")
    view.render should include ("Hi. This is Base.")
    view.render should include ("Hello World")
  }

  "A partial" should "render with escaping" in {
    val view = new MasterView

    view.render should include ("please &lt;escape&gt; me")
    view.render should include ("<div>") // prove we don't escape the partial's content
  }
}
