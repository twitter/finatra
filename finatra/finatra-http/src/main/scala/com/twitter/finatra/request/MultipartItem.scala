package com.twitter.finatra.request

case class MultipartItem(
  data: Array[Byte],  // TODO don't store file in memory; write to temp file and store file reference
  fieldName: String,
  isFormField: Boolean,
  contentType: Option[String],
  filename: Option[String])