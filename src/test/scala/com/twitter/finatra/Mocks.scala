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
