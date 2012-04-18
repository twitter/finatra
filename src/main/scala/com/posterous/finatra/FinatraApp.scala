package com.posterous.finatra
import com.codahale.logula.Logging
import com.codahale.jerkson.Json._

class FinatraApp(var prefix: String = "") extends Logging { 
     
  class TemplateBindingString(underlying:String){
    def <<(x: Any) = { 
      Router.bindTemplateVar(underlying, x) 
    }
  }
  implicit def addMyMethods(s:String)=new TemplateBindingString(s)

  def get(path: String)(callback: => Any)    { Router.addRoute("GET", prefix + path)(callback) } 
  def delete(path: String)(callback: => Any) { Router.addRoute("DELETE", prefix + path)(callback) } 
  def post(path: String)(callback: => Any)   { Router.addRoute("POST", prefix + path)(callback) } 
  def put(path: String)(callback: => Any)    { Router.addRoute("PUT", prefix + path)(callback) } 
  def head(path: String)(callback: => Any)   { Router.addRoute("HEAD", prefix + path)(callback) } 
  def patch(path: String)(callback: => Any)  { Router.addRoute("PATCH", prefix + path)(callback) } 

  def headers() = { Router.request.getHeaders }
  def header(name:String) = { Router.request.getHeader(name)}
  def params(name:String) = { Router.params(name) }
  def render(path:String) = { Router.renderTemplate(path) }
  def multiPart(name:String) = { Router.multiParams(name) }
  def response() = { Router.response }
  def request() = { Router.request }
  def headers(pair:Tuple2[String,String]) = { response.headers += pair }
  def status(code:Int) = { response.status = code }
  def contentType(mtype:String) = { response.mediaType = mtype }
  def toJson(obj: Any) = { generate(obj) }
  def redirect(url: String) = { status(301); headers("Location", url) }
}
