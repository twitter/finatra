package com.twitter.finatra

import com.twitter.finatra.test.ShouldSpec
import com.twitter.util.Await
import com.twitter.finagle.http.{Request => FinagleRequest}
import com.twitter.finagle.http.service.NullService
import org.jboss.netty.handler.codec.http.HttpResponseStatus


class FileServiceSpec extends ShouldSpec {
  val fileService = new FileService

  // We assert the content, rather than just 200, since FileService always defers to AppService to render the 404.
  "looking up static files" should "return gif content" in {
    val r = FinagleRequest("/dealwithit.gif")
    val response = fileService(r, NullService)
    Await.result(response).getContent().array().length should not equal 0
  }

  "looking up static files with params" should "return gif content" in {
    val r = FinagleRequest("/dealwithit.gif", "foo" -> "bar")
    val response = fileService(r, NullService)
    Await.result(response).getContent().array().length should not equal 0
  }

  "looking up static files" should "set Content-Length" in {
    val r = FinagleRequest("/dealwithit.gif", "foo" -> "bar")
    val response = fileService(r, NullService)
    Await.result(response).contentLength should equal (Some(422488L))
  }

  "looking up static files" should "set Last-Modified" in {
    val r = FinagleRequest("/dealwithit.gif")
    val response = fileService(r, NullService)
    Await.result(response).lastModified should not equal None
  }

  "looking up static files" should "return NOT_MODIFIED for unmodified file" in {
    val req1 = FinagleRequest("/dealwithit.gif")
    val res1 = fileService(req1, NullService)
    val lastModified = Await.result(res1).lastModified.get

    val req2 = FinagleRequest("/dealwithit.gif")
    req2.lastModified = lastModified
    val res2 = fileService(req2, NullService)
    Await.result(res2).status should equal(HttpResponseStatus.OK)
  }

  "looking up static files in production" should "set Last-Modified" in {
    System.setProperty("com.twitter.finatra.config.env", "production")
    try {
      val r = FinagleRequest("/dealwithit.gif")
      val response = fileService(r, NullService)
      Await.result(response).lastModified should not equal None
    } finally {
      System.setProperty("com.twitter.finatra.config.env", "development")
    }
  }

  "looking up static files in production" should "return NOT_MODIFIED for unmodified file" in {
    System.setProperty("com.twitter.finatra.config.env", "production")
    try {
      val req1 = FinagleRequest("/dealwithit.gif")
      val res1 = fileService(req1, NullService)
      val lastModified = Await.result(res1).lastModified.get

      val req2 = FinagleRequest("/dealwithit.gif")
      req2.lastModified = lastModified
      val res2 = fileService(req2, NullService)
      Await.result(res2).status should equal(HttpResponseStatus.OK)
    } finally {
      System.setProperty("com.twitter.finatra.config.env", "development")
    }
  }
}
