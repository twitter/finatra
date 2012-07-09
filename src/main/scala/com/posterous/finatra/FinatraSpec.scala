package com.posterous.finatra

import com.posterous.finatra._
import com.capotej.finatra_core.{AbstractFinatraSpec, FinatraRequest}
import org.jboss.netty.handler.codec.http.DefaultHttpResponse
import com.twitter.util.Future

abstract class FinatraSpec extends AbstractFinatraSpec {
  def response = lastResponse.asInstanceOf[FinatraResponse]

  override def buildRequest(method:String, path:String, params:Map[String,String]=Map(), headers:Map[String,String]=Map()) {
    val request   = new FinatraRequest(method=method,path=path,params=params,headers=headers)
    lastResponse  = app.dispatch(request).asInstanceOf[Option[Future[FinatraResponse]]].get.get()
  }
}