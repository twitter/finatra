package com.posterous.finatra

import com.codahale.logula.Logging
import scala.collection.mutable.HashSet
import com.twitter.finagle.http.{Http, RichHttp, Request, Response}

object Router extends Logging {
  var routes: HashSet[(String, PathPattern, Function0[Any])] = HashSet()

  def addRoute(method: String, path: String)(callback: => Any) {
    val regex = SinatraPathPatternParser(path)
    log.info("adding regex %s for %s", regex, path)
    routes += Tuple3(method, regex, (() => callback))
  }

  def dispatch(request: Request) = {
    val fa = new FinatraApp()
    fa(request)
    //returnFuture("asd")
  }

  def mount(app: FinatraApp) = {
    log.info("mounting %s", app)
  }
  

  def get(path: String)(callback: => Any) { addRoute("GET", path)(callback) } 
  def delete(path: String)(callback: => Any) { addRoute("DELETE", path)(callback) } 
  def post(path: String)(callback: => Any) { addRoute("POST", path)(callback) } 
  def put(path: String)(callback: => Any) { addRoute("PUT", path)(callback) } 
  def head(path: String)(callback: => Any) { addRoute("HEAD", path)(callback) } 
  def patch(path: String)(callback: => Any) { addRoute("PATCH", path)(callback) } 

}
