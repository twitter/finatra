package com.posterous.finatra

import org.apache.commons.fileupload._
import java.io._

class MultipartItem(val fileobj:Tuple2[java.util.Map[String,String], ByteArrayOutputStream]) {
  def headers() = {
    this.fileobj._1
  }

  def data() = {
    this.fileobj._2
  }

  def name() = {
    headers.get("name")
  }

  def contentType = {
    headers.get("Content-Type")
  }

  def filename = {
    headers.get("filename")
  }

  def writeToFile(path: String) = {
    val fileout = new FileOutputStream(path)
    data.writeTo(fileout)
    fileout.close
  }
}
