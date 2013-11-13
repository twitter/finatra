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
}
