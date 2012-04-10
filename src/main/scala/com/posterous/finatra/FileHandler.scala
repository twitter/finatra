package com.posterous.finatra

import com.twitter.finagle.http.{Http, RichHttp, Request, Response}
import com.twitter.finagle.http.Status._
import com.twitter.finagle.http.Version.Http11
import com.twitter.finagle.http.path._
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.util.LruMap
import com.twitter.util.Future
import org.jboss.netty.buffer.ChannelBuffers.copiedBuffer
import org.jboss.netty.util.CharsetUtil.UTF_8
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.buffer.ChannelBuffers
import java.io._

class FileHandler extends SimpleFilter[Request, Response]  {
  //more holes than swiss cheese
  def validPath(path: String):Boolean = {
    val file = new File(FinatraServer.docroot, path)
    if(file.toString.contains(".."))     return false
    if(!file.exists || file.isDirectory) return false
    if(!file.canRead)                    return false
    return true 
  }
  
  def apply(request: Request, service: Service[Request, Response]) = {
    if(validPath(request.path)){
      val file = new File(FinatraServer.docroot, request.path) 
      val fh = new FileInputStream(file)
      val b = new Array[Byte](file.length.toInt)
      fh.read(b)
      
      val response = Response(Http11, InternalServerError)
      response.setContent(copiedBuffer(b))
      Future.value(response)
    } else {
      service(request)
    }
  }

}
