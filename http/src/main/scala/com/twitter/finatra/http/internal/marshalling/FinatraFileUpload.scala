package com.twitter.finatra.http.internal.marshalling

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.fileupload.MultipartItem
import org.apache.commons.fileupload.{FileItemFactory, FileItemHeaders, FileItemIterator, FileUploadBase}
import org.apache.commons.io.IOUtils
import org.jboss.netty.handler.codec.http.HttpMethod
import scala.collection.mutable

class FinatraFileUpload extends FileUploadBase {

  def parseMultipartItems(request: Request): Map[String, MultipartItem] = {
    val multipartMap = mutable.Map[String, MultipartItem]()

    fileItemIterator(request) map { itr =>
      while (itr.hasNext) {
        val multipartItemStream = itr.next()

        val multipartItemInMemory = MultipartItem(
          data = IOUtils.toByteArray(multipartItemStream.openStream()),
          fieldName = multipartItemStream.getFieldName,
          isFormField = multipartItemStream.isFormField,
          contentType = Option(multipartItemStream.getContentType),
          filename = Option(multipartItemStream.getName),
          headers = multipartItemStream.getHeaders)

        multipartMap += multipartItemInMemory.fieldName -> multipartItemInMemory
      }
    }

    multipartMap.toMap
  }

  def fileItemIterator(request: Request): Option[FileItemIterator] = {
    if(isPost(request) && isMultipart(request))
      Some(
        getItemIterator(
          new FinatraRequestContext(request)))
    else
      None
  }

  override def setFileItemFactory(factory: FileItemFactory) {
    throw new UnsupportedOperationException("FileItemFactory is not supported.")
  }

  override def getFileItemFactory: FileItemFactory = {
    throw new UnsupportedOperationException("FileItemFactory is not supported.")
  }

  private def isMultipart(request: Request): Boolean = {
    request.contentType match {
      case Some(contentType) =>
        contentType.startsWith("multipart/")
      case _ =>
        false
    }
  }

  private def isPost(request: Request): Boolean = {
    request.getMethod() == HttpMethod.POST
  }
}
