package com.posterous.finatra

object Router {
  var routes: Map[String, Function0[Any]] = Map()

  def addRoute(method: String, path: String)(callback: => Any) {
    routes += hashKey(method, path) -> (() => callback)
  }

  def hashKey(method: String, path: String) = method + path

  def get(path: String)(callback: => Any) { addRoute("GET", path)(callback) } 
  def delete(path: String)(callback: => Any) { addRoute("DELETE", path)(callback) } 
  def post(path: String)(callback: => Any) { addRoute("POST", path)(callback) } 
  def put(path: String)(callback: => Any) { addRoute("PUT", path)(callback) } 
  def head(path: String)(callback: => Any) { addRoute("HEAD", path)(callback) } 
  def patch(path: String)(callback: => Any) { addRoute("PATCH", path)(callback) } 

}
