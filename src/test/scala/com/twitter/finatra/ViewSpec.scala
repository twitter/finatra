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

  "A layout" should "render in production mode" in {
    System.setProperty("com.twitter.finatra.config.env", "production")
    try {
      val posts = List(new Post("One"), new Post("Two"))
      val view  = new PostsListView(posts)

      view.render should include ("Hello World")
      view.render should include ("This is Base.")
    } finally {
      System.setProperty("com.twitter.finatra.config.env", "development")
    }
  }
}
