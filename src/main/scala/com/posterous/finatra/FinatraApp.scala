package com.posterous.finatra

import scala.collection.mutable.HashSet
import scala.collection.mutable.ListBuffer

// Generic controller, receive a generic request, and returns a generic response

case class GenericRequest
  (var path: String, 
   var method: String = "GET",
   var params: Map[String,String] = Map())

case class GenericResponse
  (var body: Array[Byte], 
   var headers: Map[String, String],
   var status: Int)


abstract class Controller(var prefix: String = "") {

  var routes: HashSet[(String, PathPattern, Function1[GenericRequest, Array[Byte]])] = HashSet()

  def addRoute(method: String, path: String)(callback: Function1[GenericRequest, Array[Byte]]) {
    val regex = SinatraPathPatternParser(path)
    routes += Tuple3(method, regex, callback)
  }

  def dispatch(request: GenericRequest): GenericResponse = {
    findRoute(request) match {
      case Some((method, pattern, callback)) =>
        new GenericResponse(body = callback(request), headers = Map(), status = 200)
      case None => 
        new GenericResponse(body = "Not Found".getBytes, headers = Map(), status = 404)
    }
  }

  def get(path: String)(callback: Function1[GenericRequest, Array[Byte]])    { addRoute("GET", prefix + path)(callback) }
  def delete(path: String)(callback: Function1[GenericRequest, Array[Byte]]) { addRoute("DELETE", prefix + path)(callback) }
  def post(path: String)(callback: Function1[GenericRequest, Array[Byte]])   { addRoute("POST", prefix + path)(callback) }
  def put(path: String)(callback:  Function1[GenericRequest, Array[Byte]])    { addRoute("PUT", prefix + path)(callback) }
  def head(path: String)(callback: Function1[GenericRequest, Array[Byte]])   { addRoute("HEAD", prefix + path)(callback) }
  def patch(path: String)(callback: Function1[GenericRequest, Array[Byte]])  { addRoute("PATCH", prefix + path)(callback) }

  def extractParams(request:GenericRequest, xs: Tuple2[_, _]) = {
    request.params += Tuple2(xs._1.toString, xs._2.asInstanceOf[ListBuffer[String]].head.toString)
  }
  
  def findRoute(request: GenericRequest) = {
    var thematch:Option[Map[_,_]] = None
    
    this.routes.find( route => route match {
      case (_method, pattern, callback) =>
        thematch = pattern(request.path)
        if(thematch.getOrElse(null) != null && _method == request.method) {
          thematch.getOrElse(null).foreach(xs => extractParams(request, xs))
          true
        } else {
          if(thematch.getOrElse(null) != null && _method == "GET") {
            thematch.getOrElse(null).foreach(xs => extractParams(request, xs))
            true
          } else {
            false
          }
        }
    })
  }
  
  // def headers() = { Router.request.getHeaders }
  // def header(name:String) = { Router.request.getHeader(name)}
  // def params(name:String) = { Router.params(name) }
  // def render(path:String) = { Router.renderTemplate(path) }
  // def multiPart(name:String) = { Router.multiParams(name) }
  // def response() = { Router.response }
  // def request() = { Router.request }
  // def headers(pair:Tuple2[String,String]) = { response.headers += pair }
  // def status(code:Int) = { response.status = code }
  // def contentType(mtype:String) = { response.mediaType = mtype }
  // def redirect(url: String) = { status(301); headers("Location", url) }
}
