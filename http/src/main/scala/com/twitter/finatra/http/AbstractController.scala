package com.twitter.finatra.http

import com.twitter.finagle.http.{RouteIndex, Request}

abstract class AbstractController extends Controller {

  def configureRoutes(): Unit

  /* GET/ */

  protected def get(route: String, callback: JavaCallback): Unit = {
    get(route, admin = false, callback)
  }

  protected def get(route: String, admin: Boolean, callback: JavaCallback): Unit = {
    get(route, admin, null, callback)
  }

  protected def get(route: String, admin: Boolean, index: RouteIndex, callback: JavaCallback): Unit = {
    get(route, "", admin, index, callback)
  }

  protected def get(route: String, name: String, admin: Boolean, index: RouteIndex, callback: JavaCallback): Unit = {
    get[Request, Object](route, name, admin, Option.apply(index)) { request =>
      callback.handle(request)
    }
  }

  /* POST/ */

  protected def post(route: String, callback: JavaCallback): Unit = {
    post(route, admin = false, callback)
  }

  protected def post(route: String, admin: Boolean, callback: JavaCallback): Unit = {
    post(route, admin, null, callback)
  }

  protected def post(route: String, admin: Boolean, index: RouteIndex, callback: JavaCallback): Unit = {
    post(route, "", admin, index, callback)
  }

  protected def post(route: String, name: String, admin: Boolean, index: RouteIndex, callback: JavaCallback): Unit = {
    post[Request, Object](route, name, admin, Option.apply(index)) { request =>
      callback.handle(request)
    }
  }

  /* PUT/ */

  protected def put(route: String, callback: JavaCallback): Unit = {
    put(route, admin = false, callback)
  }

  protected def put(route: String, admin: Boolean, callback: JavaCallback): Unit = {
    put(route, admin, null, callback)
  }

  protected def put(route: String, admin: Boolean, index: RouteIndex, callback: JavaCallback): Unit = {
    put(route, "", admin, index, callback)
  }

  protected def put(route: String, name: String, admin: Boolean, index: RouteIndex, callback: JavaCallback): Unit = {
    put[Request, Object](route, name, admin, Option.apply(index)) { request =>
      callback.handle(request)
    }
  }

  /* DELETE/ */

  protected def delete(route: String, callback: JavaCallback): Unit = {
    delete(route, admin = false, callback)
  }

  protected def delete(route: String, admin: Boolean, callback: JavaCallback): Unit = {
    delete(route, admin, null, callback)
  }

  protected def delete(route: String, admin: Boolean, index: RouteIndex, callback: JavaCallback): Unit = {
    delete(route, "", admin, index, callback)
  }

  protected def delete(route: String, name: String, admin: Boolean, index: RouteIndex, callback: JavaCallback): Unit = {
    delete[Request, Object](route, name, admin, Option.apply(index)) { request =>
      callback.handle(request)
    }
  }

  /* OPTIONS/ */

  protected def options(route: String, callback: JavaCallback): Unit = {
    options(route, admin = false, callback)
  }

  protected def options(route: String, admin: Boolean, callback: JavaCallback): Unit = {
    options(route, admin, null, callback)
  }

  protected def options(route: String, admin: Boolean, index: RouteIndex, callback: JavaCallback): Unit = {
    options(route, "", admin, index, callback)
  }

  protected def options(route: String, name: String, admin: Boolean, index: RouteIndex, callback: JavaCallback): Unit = {
    options[Request, Object](route, name, admin, Option.apply(index)) { request =>
      callback.handle(request)
    }
  }

  /* PATCH/ */

  protected def patch(route: String, callback: JavaCallback): Unit = {
    patch(route, admin = false, callback)
  }

  protected def patch(route: String, admin: Boolean, callback: JavaCallback): Unit = {
    patch(route, admin, null, callback)
  }

  protected def patch(route: String, admin: Boolean, index: RouteIndex, callback: JavaCallback): Unit = {
    patch(route, "", admin, index, callback)
  }

  protected def patch(route: String, name: String, admin: Boolean, index: RouteIndex, callback: JavaCallback): Unit = {
    patch[Request, Object](route, name, admin, Option.apply(index)) { request =>
      callback.handle(request)
    }
  }

  /* HEAD/ */

  protected def head(route: String, callback: JavaCallback): Unit = {
    head(route, admin = false, callback)
  }

  protected def head(route: String, admin: Boolean, callback: JavaCallback): Unit = {
    head(route, admin, null, callback)
  }

  protected def head(route: String, admin: Boolean, index: RouteIndex, callback: JavaCallback): Unit = {
    head(route, "", admin, index, callback)
  }

  protected def head(route: String, name: String, admin: Boolean, index: RouteIndex, callback: JavaCallback): Unit = {
    head[Request, Object](route, name, admin, Option.apply(index)) { request =>
      callback.handle(request)
    }
  }

  /* TRACE/ */

  protected def trace(route: String, callback: JavaCallback): Unit = {
    trace(route, admin = false, callback)
  }

  protected def trace(route: String, admin: Boolean, callback: JavaCallback): Unit = {
    trace(route, admin, null, callback)
  }

  protected def trace(route: String, admin: Boolean, index: RouteIndex, callback: JavaCallback): Unit = {
    trace(route, "", admin, index, callback)
  }

  protected def trace(route: String, name: String,  admin: Boolean, index: RouteIndex, callback: JavaCallback): Unit = {
    trace[Request, Object](route, name, admin, Option.apply(index)) { request =>
      callback.handle(request)
    }
  }

  /* ANY/ */

  protected def any(route: String, callback: JavaCallback): Unit = {
    any(route, admin = false, callback)
  }

  protected def any(route: String, admin: Boolean, callback: JavaCallback): Unit = {
    any(route, admin, null, callback)
  }

  protected def any(route: String, admin: Boolean, index: RouteIndex, callback: JavaCallback): Unit = {
    any(route, "", admin, index, callback)
  }

  protected def any(route: String, name: String, admin: Boolean, index: RouteIndex, callback: JavaCallback): Unit = {
    any[Request, Object](route, name, admin, Option.apply(index)) { request =>
      callback.handle(request)
    }
  }
}
