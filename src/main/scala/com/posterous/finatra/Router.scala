package com.posterous.finatra
import scala.collection.mutable.HashSet

object Router {
  var routes: HashSet[(String, PathPattern, Function0[Any])] = HashSet()

  def addRoute(method: String, path: String)(callback: => Any) {
    val regex = SinatraPathPatternParser(path)
    routes += Tuple3(method, regex, (() => callback))
  }
  

  def get(path: String)(callback: => Any) { addRoute("GET", path)(callback) } 
  def delete(path: String)(callback: => Any) { addRoute("DELETE", path)(callback) } 
  def post(path: String)(callback: => Any) { addRoute("POST", path)(callback) } 
  def put(path: String)(callback: => Any) { addRoute("PUT", path)(callback) } 
  def head(path: String)(callback: => Any) { addRoute("HEAD", path)(callback) } 
  def patch(path: String)(callback: => Any) { addRoute("PATCH", path)(callback) } 

}
