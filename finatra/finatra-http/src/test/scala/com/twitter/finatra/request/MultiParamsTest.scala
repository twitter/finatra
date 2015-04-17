package com.twitter.finatra.request

import com.twitter.finagle.http.Request
import com.twitter.finagle.{http => finagle}
import com.twitter.finatra.fileupload.MultipartItem
import com.twitter.inject.Test
import org.apache.commons.io.IOUtils
import org.jboss.netty.handler.codec.http.HttpMethod
import org.specs2.mock.Mockito

class MultiParamsTest extends Test with Mockito {

  "multipart parsing" should {

    "handle mixed multipart" in {

      /*
        'upload.bytes' is a serialized request of a multipart upload from Chrome using the form:
          <form enctype="multipart/form-data" action="/groups_file?debug=true" method="POST">
            <label for="groups">Filename:</label>
            <input type="file" name="groups" id="groups"><br>
            <input type="hidden" name="type" value="text"/>
            <input type="submit" name="submit" value="Submit">
          </form>

        The infamous 'dealwithit.gif' was used for the "groups" file
      */

      val fileUploadFileBytes = resourceAsBytes("/multipart/dealwithit.gif")
      val requestAsBytes = resourceAsBytes("/multipart/upload.bytes")
      val finagleRequest = finagle.Request.decodeBytes(requestAsBytes)

      val expectedMultiParams = Map[String, MultipartItem](

        "type" -> MultipartItem(
          data = "text".getBytes("utf-8"),
          fieldName = "type",
          isFormField = true,
          contentType = None,
          filename = None),

        "submit" -> MultipartItem(
          data = "Submit".getBytes("utf-8"),
          fieldName = "submit",
          isFormField = true,
          contentType = None,
          filename = None),

        "groups" -> MultipartItem(
          data = fileUploadFileBytes,
          fieldName = "groups",
          isFormField = false,
          contentType = Some("image/gif"),
          filename = Some("dealwithit.gif")))

      assertMultiParams(finagleRequest, expectedMultiParams)
    }

    "handle MS Surface Upload with quoted boundary" in {
      val fileUploadFileBytes = resourceAsBytes("/multipart/TempProfileImageCrop.png")
      val requestAsBytes = resourceAsBytes("/multipart/ms-surface.bytes")
      val finagleRequest = finagle.Request.decodeBytes(requestAsBytes)

      val expectedMultiParams = Map[String, MultipartItem](
        "banner" -> MultipartItem(
          data = fileUploadFileBytes,
          fieldName = "banner",
          isFormField = false,
          contentType = None,
          filename = Some("TempProfileImageCrop.png")))

      assertMultiParams(finagleRequest, expectedMultiParams)
    }

    "handle Iphone Upload with multiple boundaries" in {
      val fileUploadFileBytes = resourceAsBytes("/multipart/image.jpg")
      val requestAsBytes = resourceAsBytes("/multipart/request-POST-iphone.bytes")
      val finagleRequest = finagle.Request.decodeBytes(requestAsBytes)

      val expectedMultiParams = Map[String, MultipartItem](

        "offset_top" -> MultipartItem(
          data = "0".getBytes("utf-8"),
          fieldName = "offset_top",
          isFormField = true,
          contentType = None,
          filename = None),

        "offset_left" -> MultipartItem(
          data = "0".getBytes("utf-8"),
          fieldName = "offset_left",
          isFormField = true,
          contentType = None,
          filename = None),

        "height" -> MultipartItem(
          data = "626".getBytes("utf-8"),
          fieldName = "height",
          isFormField = true,
          contentType = None,
          filename = None),

        "width" -> MultipartItem(
          data = "1252".getBytes("utf-8"),
          fieldName = "width",
          isFormField = true,
          contentType = None,
          filename = None),

        "true" -> MultipartItem(
          data = "include_user_entities".getBytes("utf-8"),
          fieldName = "true",
          isFormField = true,
          contentType = None,
          filename = None),

        "banner" -> MultipartItem(
          data = fileUploadFileBytes,
          fieldName = "banner",
          isFormField = false,
          contentType = Some("image/jpeg"),
          filename = Some("image.jpg")))

      assertMultiParams(finagleRequest, expectedMultiParams)
    }

    "Android Upload with multiple boundaries" in {
      val fileUploadFileBytes = resourceAsBytes("/multipart/kM1K5C4p")
      val requestAsBytes = resourceAsBytes("/multipart/request-POST-android.bytes")
      val finagleRequest = finagle.Request.decodeBytes(requestAsBytes)

      val expectedMultiParams = Map[String, MultipartItem](
        "banner" -> MultipartItem(
          data = fileUploadFileBytes,
          fieldName = "banner",
          isFormField = false,
          contentType = Some("image/jpeg"),
          filename = Some("kM1K5C4p")))

      assertMultiParams(finagleRequest, expectedMultiParams)
    }

    "invalid upload data" in {
      val requestAsBytes = resourceAsBytes("/multipart/request-POST-android.bytes")
      val finagleRequest = finagle.Request.decodeBytes(requestAsBytes)
      finagleRequest.setContentType("text/html; bounfoodary=foo; charset=UTF-8\"")

      assertMultiParamsEmpty(finagleRequest)
    }

    "return empty map if not post" in {
      val mockFinagleRequest = mock[finagle.Request]
      mockFinagleRequest.method returns HttpMethod.GET

      assertMultiParamsEmpty(mockFinagleRequest)
    }

    "return empty map if post, but not multipart content type" in {
      val mockFinagleRequest = mock[finagle.Request]
      mockFinagleRequest.method returns HttpMethod.POST
      mockFinagleRequest.contentType returns None

      assertMultiParamsEmpty(mockFinagleRequest)
    }
  }

  private def resourceAsBytes(resource: String) = {
    IOUtils.toByteArray(
      getClass.getResourceAsStream(resource))
  }

  private def assertMultiParams(finagleRequest: Request, expectedMultiParams: Map[String, MultipartItem]) {
    multipartParamsEquals(
      RequestUtils.multiParams(finagleRequest), expectedMultiParams)
  }

  private def assertMultiParamsEmpty(finagleRequest: Request) {
    RequestUtils.multiParams(finagleRequest).size should be(0)
  }

  private def multipartParamsEquals(actual: Map[String, MultipartItem], expected: Map[String, MultipartItem]) {
    actual.size should equal(expected.size)
    for ((actualName, actualMultiParam) <- actual) {
      multipartItemEquals(actualMultiParam, expected(actualName))
    }
  }

  private def multipartItemEquals(actual: MultipartItem, expected: MultipartItem) {
    actual.fieldName should equal(expected.fieldName)
    actual.isFormField should equal(expected.isFormField)
    actual.contentType should equal(expected.contentType)
    actual.filename should equal(expected.filename)

    // need to convert from Array[Byte] to Seq[Byte] for equality check
    // TODO make 'should equal" with with MultipartItem case class
    actual.data.toSeq should equal(expected.data.toSeq)
  }
}