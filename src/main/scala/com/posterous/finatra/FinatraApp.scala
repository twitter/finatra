package com.posterous.finatra

import scala.collection.mutable.HashSet
import scala.collection.mutable.ListBuffer

// Generic controller, receive a generic request, and returns a generic response

case class GenericRequest(var path:String)
case class GenericResponse(var body:Array[Byte], var headers: Map[String, String], var status: Int)

abstract class Controller(var prefix: String = "") {

  var routes: HashSet[(String, PathPattern, Function0[Any])] = HashSet()

  def addRoute(method: String, path: String)(callback: => Any) {
    val regex = SinatraPathPatternParser(path)
    routes += Tuple3(method, regex, (() => callback))
  }

  def dispatch(request: GenericRequest): GenericResponse = {
    new GenericResponse(body = "resp".getBytes, headers = Map(), status = 200)  
  }

  def get(path: String)(callback: => Any)    { addRoute("GET", prefix + path)(callback) }
  def delete(path: String)(callback: => Any) { addRoute("DELETE", prefix + path)(callback) }
  def post(path: String)(callback: => Any)   { addRoute("POST", prefix + path)(callback) }
  def put(path: String)(callback: => Any)    { addRoute("PUT", prefix + path)(callback) }
  def head(path: String)(callback: => Any)   { addRoute("HEAD", prefix + path)(callback) }
  def patch(path: String)(callback: => Any)  { addRoute("PATCH", prefix + path)(callback) }

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
