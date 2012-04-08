package com.posterous.finatra

import org.apache.commons.fileupload._
import java.io._
import com.twitter.finagle.http.{Http, RichHttp, Request, Response}
import org.jboss.netty.util.CharsetUtil.UTF_8

// HERE BE DRAGONS

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

object MultipartParsing {
  
  def loadMultiParams(request: Request) = {
    
    var multiParams = Map[String, MultipartItem]()
    val ctype = request.headers.getOrElse("Content-Type", null)
    if(ctype != null){
      val boundaryIndex = ctype.indexOf("boundary=");
      val boundary = ctype.substring(boundaryIndex + 9).getBytes
      println(request.getContentLength)
      val input = new ByteArrayInputStream(request.getContent.array) 

      try {
        val multistream = new MultipartStream(input, boundary)
        var nextPart = multistream.skipPreamble
        while(nextPart){
          val paramParser = new ParameterParser
          val headers = paramParser.parse(multistream.readHeaders.toString, ';').asInstanceOf[java.util.Map[String,String]]
          val out = new ByteArrayOutputStream
          // FILE WORKED HERE val fout = new FileOutputStream("/tmp/lol1.jpg")
          val name = headers.get("name").toString
          multistream.readBodyData(fout)
          val fileobj = new MultipartItem(Tuple2(headers, out))
          multiParams = multiParams + Tuple2(name, fileobj)
          nextPart = multistream.readBoundary
        }
      } catch {
          case e: MultipartStream.MalformedStreamException => println("wrong") 
          case e: IOException => println("error") 
      }
    }
    multiParams
  }


}
