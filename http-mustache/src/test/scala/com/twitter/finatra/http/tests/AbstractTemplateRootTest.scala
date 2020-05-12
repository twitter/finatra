package com.twitter.finatra.http.tests

import com.twitter.finagle.http.Status.{Created, Ok}
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.finatra.http.tests.server.TestHttpServer
import com.twitter.finatra.http.tests.server.views.TestUserView
import com.twitter.finatra.modules.FileResolverFlags
import com.twitter.finatra.mustache.marshalling.MustacheService
import com.twitter.finatra.mustache.modules.MustacheFlags
import com.twitter.inject.Test
import com.twitter.io.TempFolder
import java.io.FileWriter

abstract class AbstractTemplateRootTest extends Test with FileUtility with TempFolder {

  def templateRootDirectory(baseFolderName: String): String

  def fullTemplatePath(baseFolderName: String): String

  def setup(baseFolderName: String): Unit

  test(s"${this.getClass.getSimpleName}#test all cases") {
    withTempFolder {
      setup(folderName)

      val server = new EmbeddedHttpServer(
        flags = Map(
          FileResolverFlags.LocalDocRoot -> s"${folderName}src/main/webapp",
          MustacheFlags.TemplatesDirectory -> templateRootDirectory(folderName)
        ),
        twitterServer = new TestHttpServer,
        disableTestLogging = true
      )

      try {
        server.httpGet(
          "/getView?age=18&name=bob",
          andExpect = Ok,
          withBody = "age:18\nname:bob\nuser1\nuser2\n"
        )

        server.httpFormPost(
          "/formPostViewFromBuilderView",
          params = Map("name" -> "bob", "age" -> "18"),
          andExpect = Ok,
          withBody = "age2:18\nname2:bob\nuser1\nuser2\n"
        )

        server.httpFormPost(
          "/formPostViewFromBuilderHtml",
          params = Map("name" -> "bob", "age" -> "18"),
          andExpect = Ok,
          withBody = "age:18\nname:bob\nuser1\nuser2\n"
        )

        server
          .httpFormPost(
            "/formPostViewFromBuilderCreatedView",
            params = Map("name" -> "bob", "age" -> "18"),
            andExpect = Created,
            withBody = "age2:18\nname2:bob\nuser1\nuser2\n"
          ).location should equal(Some("/foo/1"))

        server
          .httpFormPost(
            "/formPostViewFromBuilderCreatedHtml",
            params = Map("name" -> "bob", "age" -> "18"),
            andExpect = Created,
            withBody = "age:18\nname:bob\nuser1\nuser2\n"
          ).location should equal(Some("/foo/1"))

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
        """.stripMargin
        )

        // test support un-cached templates
        val testUser = TestUserView(28, "Bob Smith", Seq("user1", "user2"))
        val mustacheService = server.injector.instance[MustacheService]
        val firstResult = mustacheService.createString("testuser.mustache", testUser)
        firstResult should be("age:28\nname:Bob Smith\nuser1\nuser2\n")

        // alter the file
        val testUserMustacheFile =
          new FileWriter(s"${fullTemplatePath(folderName)}/testuser.mustache")
        try {
          testUserMustacheFile.write("")
          testUserMustacheFile.append(
            "another age:{{age}}\nanother name:{{name}}\n{{#friends}}\n{{.}}\n{{/friends}}"
          )
        } finally {
          testUserMustacheFile.close()
        }

        val alteredResult = mustacheService.createString("testuser.mustache", testUser)
        alteredResult should be("another age:28\nanother name:Bob Smith\nuser1\nuser2\n")
      } finally {
        server.close()
      }
    }
  }
}
