package com.twitter.finatra.test


class LayoutViewSpec extends ShouldSpec {
  "A LayoutView" should "render" in {

    val posts   = List(new Post("One"), new Post("Two"))
    val layout  = new PostsView(posts)

    layout.render should include ("Posts")
    layout.render should include ("Title: One")
  }
}
