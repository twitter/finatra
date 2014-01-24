package com.twitter.finatra

import com.twitter.finatra.test.ShouldSpec
import com.twitter.util.Await
import com.twitter.finagle.http.{Request => FinagleRequest}
import com.twitter.finagle.http.service.NullService


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
}
