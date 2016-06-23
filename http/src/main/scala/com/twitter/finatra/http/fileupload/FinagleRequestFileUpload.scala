package com.twitter.finatra.http.fileupload

import com.twitter.finagle.http.{Method, Request}
import com.twitter.finatra.http.internal.marshalling.FinatraRequestContext
import org.apache.commons.fileupload.{FileItemFactory, FileItemIterator, FileUploadBase}
import org.apache.commons.io.IOUtils
import scala.collection.mutable

class FinagleRequestFileUpload extends FileUploadBase {

  def parseMultipartItems(request: Request): Map[String, MultipartItem] = {
    val multipartMap = mutable.Map[String, MultipartItem]()

    fileItemIterator(request) foreach { itr =>
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
    if(isPostOrPut(request) && isMultipart(request))
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

  private def isPostOrPut(request: Request): Boolean = {
    Method.Post == request.method ||
    Method.Put == request.method
  }
}
