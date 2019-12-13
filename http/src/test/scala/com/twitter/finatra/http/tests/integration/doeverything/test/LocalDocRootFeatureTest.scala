package com.twitter.finatra.http.tests.integration.doeverything.test

import com.twitter.finagle.http.Status._
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.finatra.http.tests.integration.doeverything.main.DoEverythingServer
import com.twitter.finatra.test.LocalFilesystemTestUtils._
import com.twitter.inject.server.FeatureTest
import java.io.File

class LocalDocRootFeatureTest extends FeatureTest with DocRootLocalFilesystemTestUtility {

  override val server = new EmbeddedHttpServer(
    flags = Map(
      "local.doc.root" -> s"${BaseDirectory}src/main/webapp",
      "something.flag" -> "foobar"
    ),
    args = Array("-magicNum=1", "-moduleMagicNum=2"),
    twitterServer = new DoEverythingServer
  )

  override protected def beforeAll(): Unit = {
    super.beforeAll()

    // create src/main/webapp directory and add files
    val webapp = createFile(s"${BaseDirectory}src/main/webapp")
    writeStringToFile(createFile(webapp, "testfile.txt"), testFileText)
    writeStringToFile(createFile(webapp, "testindex.html"), testIndexHtml)
  }

  override protected def afterAll(): Unit = {
    // try to help clean up
    new File(s"${BaseDirectory}src").delete
    super.afterAll()
  }

  test("DoEverythingServer#testfile") {
    server.httpGet("/testfile", andExpect = Ok, withBody = "testfile123")
  }

  test("DoEverythingServer#testfile when not found") {
    server.httpGet(
      "/testfileWhenNotfound",
      andExpect = NotFound,
      withBody = "/doesntexist.txt not found"
    )
  }

  test("DoEverythingServer#index root") {
    server.httpGet("/index/", andExpect = Ok, withBody = "testindex")
  }

  test("DoEverythingServer#index file without extension") {
    server.httpGet("/index/testfile", andExpect = Ok, withBody = "testindex")
  }

  test("DoEverythingServer#index file with extension") {
    server.httpGet("/index/testfile.txt", andExpect = Ok, withBody = "testfile123")
  }
}
