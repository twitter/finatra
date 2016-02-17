package com.twitter.finatra.http

import com.twitter.finagle.http.Request

abstract class JavaController extends Controller {

  def configureRoutes(): Unit

  protected def get(route: String, callback: JavaCallback) {
    get[Request, Object](route) { request =>
      callback.handle(request)
    }
  }

  protected def post(route: String, callback: JavaCallback) {
    post[Request, Object](route) { request =>
      callback.handle(request)
    }
  }

  protected def put(route: String, callback: JavaCallback) {
    put[Request, Object](route) { request =>
      callback.handle(request)
    }
  }

  protected def delete(route: String, callback: JavaCallback) {
    delete[Request, Object](route) { request =>
      callback.handle(request)
    }
  }

  protected def options(route: String, callback: JavaCallback) {
    options[Request, Object](route) { request =>
      callback.handle(request)
    }
  }

  protected def patch(route: String, callback: JavaCallback) {
    patch[Request, Object](route) { request =>
      callback.handle(request)
    }
  }

  protected def head(route: String, callback: JavaCallback) {
    head[Request, Object](route) { request =>
      callback.handle(request)
    }
  }

  protected def trace(route: String, callback: JavaCallback) {
    trace[Request, Object](route) { request =>
      callback.handle(request)
    }
  }
}
