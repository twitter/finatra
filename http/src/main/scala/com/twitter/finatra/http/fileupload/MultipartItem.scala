package com.twitter.finatra.http.fileupload

import org.apache.commons.fileupload.FileItemHeaders

case class MultipartItem(
  data: Array[Byte],  // TODO don't store file in memory; write to temp file and store file reference
  fieldName: String,
  isFormField: Boolean,
  contentType: Option[String],
  filename: Option[String],
  headers: FileItemHeaders)
