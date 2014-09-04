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

import com.twitter.finatra.View

case class Post(val title:String)

class PostsListView(val posts:List[Post]) extends View {
  val template  = "posts.mustache"
  contentType   = Some("text/plain")
}

class PostsView(val posts:List[Post]) extends View {
  val template      = "posts_layout.mustache"
  val postsListView = new PostsListView(posts)

  def body          = postsListView.render
}

class MasterView extends View {
  val template = "master.mustache"
  val body = "please <escape> me"
}
