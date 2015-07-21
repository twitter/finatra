package com.twitter.finatra

package object fileupload {
  @deprecated("Use com.twitter.finatra.http.fileupload.FileUploadException", "")
  type FileUploadException = com.twitter.finatra.http.fileupload.FileUploadException

  @deprecated("Use com.twitter.finatra.http.fileupload.MultipartItem", "")
  type MultipartItem = com.twitter.finatra.http.fileupload.MultipartItem

  @deprecated("Use com.twitter.finatra.http.fileupload.MultipartItem", "")
  val MultipartItem = com.twitter.finatra.http.fileupload.MultipartItem
}
