package com.twitter.finatra.http.integration.doeverything.test

import com.twitter.finagle.http.Status._
import com.twitter.finatra.http.integration.doeverything.main.DoEverythingServer
import com.twitter.finatra.http.test.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest
import java.io.File
import org.apache.commons.io.FileUtils

class OverlappingRootsDoEverythingServerFeatureTest
  extends FeatureTest
  with LocalFilesystemTestUtility {

  override protected def beforeAll() = {
    super.beforeAll()

    // create src/main/webapp directory and add files
    val webapp = createFile(s"${baseDirectory}src/main/webapp")
    FileUtils.writeStringToFile(createFile(webapp, "testfile.txt"), testFileText)
    FileUtils.writeStringToFile(createFile(webapp, "testindex.html"), testIndexHtml)

    // create /templates directory *under* webapp and add files
    val templates = createFile(webapp, "templates")
    FileUtils.writeStringToFile(createFile(templates, "testuser.mustache"), testUserMustacheString)
    FileUtils.writeStringToFile(createFile(templates, "testuser2.mustache"), testUser2MustacheString)
    FileUtils.writeStringToFile(createFile(templates, "testHtml.mustache"), testHtmlMustacheString)
  }

  override protected def afterAll() = {
    // try to help clean up
    new File(s"${baseDirectory}src").delete
    super.afterAll()
  }

  override val server = new EmbeddedHttpServer(
    clientFlags = Map(
      "local.doc.root" -> s"${baseDirectory}src/main/webapp",
      "mustache.templates.dir" -> s"/templates"),
    extraArgs = Array("-magicNum=1", "-moduleMagicNum=2"),
    twitterServer = new DoEverythingServer)

  "DoEverythingServer" should {

    "getView" in {
      server.httpGet(
        "/getView?age=18&name=bob",
        andExpect = Ok,
        withBody = "age:18\nname:bob\nuser1\nuser2\n")
    }

    "formPostViewFromBuilderViewWithDiffTemplateThanAnnotation" in {
      server.httpFormPost(
        "/formPostViewFromBuilderView",
        params = Map("name" -> "bob", "age" -> "18"),
        andExpect = Ok,
        withBody = "age2:18\nname2:bob\nuser1\nuser2\n")
    }

    "formPostViewFromBuilderHtml" in {
      server.httpFormPost(
        "/formPostViewFromBuilderHtml",
        params = Map("name" -> "bob", "age" -> "18"),
        andExpect = Ok,
        withBody = "age:18\nname:bob\nuser1\nuser2\n")
    }

    "formPostViewFromBuilderCreatedView" in {
      val response = server.httpFormPost(
        "/formPostViewFromBuilderCreatedView",
        params = Map("name" -> "bob", "age" -> "18"),
        andExpect = Created,
        withBody = "age2:18\nname2:bob\nuser1\nuser2\n")

      response.location should equal(Some("/foo/1"))
    }

    "formPostViewFromBuilderCreatedHtml" in {
      val response = server.httpFormPost(
        "/formPostViewFromBuilderCreatedHtml",
        params = Map("name" -> "bob", "age" -> "18"),
        andExpect = Created,
        withBody = "age:18\nname:bob\nuser1\nuser2\n")

      response.location should equal(Some("/foo/1"))
    }

    "testfile" in {
      server.httpGet(
        "/testfile",
        andExpect = Ok,
        withBody = "testfile123")
    }

    "testfile when not found" in {
      server.httpGet(
        "/testfileWhenNotfound",
        andExpect = NotFound,
        withBody = "/doesntexist.txt not found")
    }

    "index root" in {
      server.httpGet(
        "/index/",
        andExpect = Ok,
        withBody = "testindex")
    }

    "index file without extension" in {
      server.httpGet(
        "/index/testfile",
        andExpect = Ok,
        withBody = "testindex")
    }

    "index file with extension" in {
      server.httpGet(
        "/index/testfile.txt",
        andExpect = Ok,
        withBody = "testfile123")
    }

    "TestCaseClassWithHtml" in {
      server.httpGet(
        "/testClassWithHtml",
        andExpect = Ok,
        withJsonBody =
          """
          |{
          |  "address" : "123 Main St. Anywhere, CA US 90210",
          |  "phone" : "+12221234567",
          |  "rendered_html" : "&lt;div class=&quot;nav&quot;&gt;\n  &lt;table cellpadding=&quot;0&quot; cellspacing=&quot;0&quot;&gt;\n    &lt;tr&gt;\n        &lt;th&gt;Name&lt;/th&gt;\n        &lt;th&gt;Age&lt;/th&gt;\n        &lt;th&gt;Friends&lt;/th&gt;\n    &lt;/tr&gt;\n    &lt;tr&gt;\n        &lt;td&gt;age2:28&lt;/td&gt;\n        &lt;td&gt;name:Bob Smith&lt;/td&gt;\n        &lt;td&gt;\n            user1\n            user2\n        &lt;/td&gt;\n    &lt;/tr&gt;\n  &lt;/table&gt;\n&lt;/div&gt;"
          |}
        """.
            stripMargin)
    }
  }
}
