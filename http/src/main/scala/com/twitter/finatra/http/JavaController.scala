package com.twitter.finatra.http

import com.twitter.finagle.http.Request

abstract class JavaController extends Controller {

  def configureRoutes(): Unit

  protected def get(route: String, callback: JavaCallback): Unit = {
    get[Request, Object](route) { request =>
      callback.handle(request)
    }
  }

  protected def post(route: String, callback: JavaCallback): Unit = {
    post[Request, Object](route) { request =>
      callback.handle(request)
    }
  }

  protected def put(route: String, callback: JavaCallback): Unit = {
    put[Request, Object](route) { request =>
      callback.handle(request)
    }
  }

  protected def delete(route: String, callback: JavaCallback): Unit = {
    delete[Request, Object](route) { request =>
      callback.handle(request)
    }
  }

  protected def options(route: String, callback: JavaCallback): Unit = {
    options[Request, Object](route) { request =>
      callback.handle(request)
    }
  }

  protected def patch(route: String, callback: JavaCallback): Unit = {
    patch[Request, Object](route) { request =>
      callback.handle(request)
    }
  }

  protected def head(route: String, callback: JavaCallback): Unit = {
    head[Request, Object](route) { request =>
      callback.handle(request)
    }
  }

  protected def trace(route: String, callback: JavaCallback): Unit = {
    trace[Request, Object](route) { request =>
      callback.handle(request)
    }
  }

  protected def any(route: String, callback: JavaCallback): Unit = {
    any[Request, Object](route) { request =>
      callback.handle(request)
    }
  }
}
