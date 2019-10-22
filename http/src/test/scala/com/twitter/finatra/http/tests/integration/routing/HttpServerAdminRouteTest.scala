package com.twitter.finatra.http.tests.integration.routing

import com.twitter.finagle.http._
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.http.{Controller, EmbeddedHttpServer, HttpServer}
import com.twitter.inject.Test

class HttpServerAdminRouteTest extends Test {

  test("Server#not allow conflicting admin routes") {
    val server = new EmbeddedHttpServer(
      twitterServer = new HttpServer {
        override protected def configureHttp(router: HttpRouter): Unit = {
          router.add(new Controller {
            get("/healthy", name = "Health", admin = true) { _: Request =>
              response.ok("OK\n")
            }

            get("/quitquitquit", name = "Quit", admin = true) { _: Request =>
              response.ok("go away")
            }

            post("/abortabortabort", admin = true) { _: Request =>
              response.ok("go away now")
            }
          })
        }
      },
      disableTestLogging = true
    )

    try {
      // defines overlapping admin route paths
      intercept[java.lang.AssertionError] {
        server.start()
      }
    } finally {
      server.close()
    }
  }

  test("Server#properly add routes to admin index") {
    val server = new EmbeddedHttpServer(
      twitterServer = new HttpServer {
        override protected def configureHttp(router: HttpRouter): Unit = {
          router.add(new Controller {
            get(
              "/admin/finatra/stuff/:id",
              admin = true,
              index = Some(RouteIndex(alias = "", group = "Finatra"))
            ) { request: Request =>
              response.ok.json(request.params("id"))
            }

            get("/admin/finatra/foo", index = Some(RouteIndex(alias = "", group = "Finatra"))) {
              _: Request =>
                response.ok.json("bar")
            }

            post("/admin/resource", admin = true) { _: Request =>
              response.ok("affirmative")
            }

            get(
              "/admin/thisisacustompath",
              admin = true,
              index = Some(RouteIndex(alias = "Custom", group = "My Service"))
            ) { _: Request =>
              response.ok.json("pong")
            }

            get("/admin/externalroute") { _: Request =>
              response.ok.json("external")
            }

            post("/special/admin/route", admin = true) { _: Request =>
              response.created.location("/special/admin/foo")
            }

            get(
              "/another/admin/route",
              admin = true,
              index = Some(RouteIndex(alias = "Special", group = "My Service"))
            ) { _: Request =>
              response.ok("pong")
            }

            post(
              "/admin/finatra/ok/computer",
              admin = true,
              index = Some(RouteIndex(alias = "", group = "Finatra"))
            ) { _: Request =>
              response.ok("roger.")
            }

            get("/admin/threadName", admin = true) {
              _: Request =>
                info("in /admin/threadName")
                response.ok(Thread.currentThread.getName)
            }

            get("/admin/finatra/threadName", admin = true) {
              _: Request =>
                info("in /admin/finatra/threadName")
                response.ok(Thread.currentThread.getName)
            }
          })
        }
      },
      disableTestLogging = true
    )

    try {
      server.start()

      // GET /admin/finatra/stuff/:id non-constant route should not be added
      server.adminHttpServerRoutes.map(_.path).contains("/admin/finatra/stuff/:id") should be(false)

      // POST /admin/finatra/ok/computer constant route should not be added
      server.adminHttpServerRoutes.map(_.path).contains("/admin/finatra/ok/computer") should be(
        false
      )

      // GET /admin/finatra/foo constant route should not be added (starts with /admin/finatra so won't be in the HttpMuxer)
      server.adminHttpServerRoutes.map(_.path).contains("/admin/finatra/foo") should be(false)

      // GET /admin/externalroute constant route not be added (does not specify admin = true)
      server.adminHttpServerRoutes.map(_.path).contains("/admin/externalroute") should be(false)

      // GET /admin/thisisacustompath constant route should be added
      val adminCustomRoute = server.adminHttpServerRoutes.find(_.path == "/admin/thisisacustompath")
      adminCustomRoute.isDefined should be(true)
      adminCustomRoute.get.includeInIndex should be(true)
      adminCustomRoute.get.group should be(Some("My Service"))
      adminCustomRoute.get.alias should be("Custom")

      // POST /admin/resource constant route should be added
      val adminResourceRoute = server.adminHttpServerRoutes.find(_.path == "/admin/resource")
      adminResourceRoute.isDefined should be(true)
      adminResourceRoute.get.includeInIndex should be(false)

      // POST /special/admin/route should be added
      val adminSpecialRoute = server.adminHttpServerRoutes.find(_.path == "/special/admin/route")
      adminSpecialRoute.isDefined should be(true)
      adminResourceRoute.get.includeInIndex should be(false) // does not define an index

      // GET /another/admin/route should be added
      val adminAnotherRoute = server.adminHttpServerRoutes.find(_.path == "/another/admin/route")
      adminAnotherRoute.isDefined should be(true)
      adminAnotherRoute.get.includeInIndex should be(false) // not indexable as it needs to start with /admin to be indexed

      // A handler should show up on the HttpMuxer
      HttpMuxer.patterns.contains("/admin/finatra/") should be(true)

      server.httpGet("/admin/threadName", andExpect = Status.Ok).getContentString() should startWith("AdminFuturePool")
      server.httpGet("/admin/finatra/threadName", andExpect = Status.Ok).getContentString() should startWith("AdminFuturePool")
    } finally {
      server.close()
    }
  }

  test("Server#Add HttpMuxer handler for /admin/finatra and non-constant or non-GET routes") {
    // if we only add non-constant routes to the admin there should still be a handler
    // registered on the HttpMuxer
    val server = new EmbeddedHttpServer(
      twitterServer = new HttpServer {
        override protected def configureHttp(router: HttpRouter): Unit = {
          router.add(new Controller {
            get("/admin/finatra/prefix/resource/:id") { request: Request =>
              response.ok.json(request.params("id"))
            }
          })
        }
      },
      disableTestLogging = true
    )

    try {
      server.start()

      // A handler should show up on the HttpMuxer
      HttpMuxer.patterns.contains("/admin/finatra/") should be(true)
    } finally {
      server.close()
    }
  }

  test("Server#Non-constant admin route does not start") {
    val server = new EmbeddedHttpServer(
      twitterServer = new HttpServer {
        override protected def configureHttp(router: HttpRouter): Unit = {
          router.add(new Controller {
            get("/prefix/resource/:id", admin = true) { request: Request =>
              response.ok.json(request.params("id"))
            }
          })
        }
      },
      disableTestLogging = true
    )

    try {
      // non-constant admin routes not supported
      intercept[java.lang.AssertionError] {
        server.start()
      }
    } finally {
      server.close()
    }
  }

  test("Server#Similar paths but differing Http Methods") {
    val server = new EmbeddedHttpServer(
      twitterServer = new HttpServer {
        override protected def configureHttp(router: HttpRouter): Unit = {
          router.add(new Controller {

            post("/foo/bar") { _: Request =>
              response.ok.location("baz")
            }

            get("/foo/bar", admin = true) { _: Request =>
              "baz"
            }

            post("/great/work", admin = true) { _: Request =>
              "this is an admin POST route"
            }

            get("/great/work") { _: Request =>
              "this is an external GET route"
            }
          })
        }
      },
      disableTestLogging = true
    )

    try {
      server.start()

      server.adminHttpServerRoutes
        .map(_.path)
        .contains("/foo/bar") should be(true) // a route with the path is there
      server.adminHttpServerRoutes.exists(
        route => route.path == "/foo/bar" && route.method == Method.Get
      ) should be(true) // however, we should also check the Method
      server.adminHttpServerRoutes.exists(
        route => route.path == "/foo/bar" && route.method == Method.Post
      ) should be(false) // in this case, the path with a Post does not exist

      server.adminHttpServerRoutes
        .map(_.path)
        .contains("/great/work") should be(true) // a route with the path is there
      server.adminHttpServerRoutes.exists(
        route => route.path == "/great/work" && route.method == Method.Post
      ) should be(true) // however, we should also check the Method
      server.adminHttpServerRoutes.exists(
        route => route.path == "/great/work" && route.method == Method.Get
      ) should be(false) // in this case, the path with a Get does not exist

      server.httpPost("/foo/bar", "", andExpect = Status.Ok, withLocation = "baz")

      server.httpGet("/foo/bar", andExpect = Status.Ok, withBody = "baz")

    } finally {
      server.close()
    }
  }

  /* Note that tests against the Global Singleton HttpMuxer break when multiple servers are started in process */
  test("Explicitly and implicitly added /admin/finatra routes") {
    val server = new EmbeddedHttpServer(
      twitterServer = new HttpServer {
        override protected def configureHttp(router: HttpRouter): Unit = {
          router.add(new Controller {
            get("/admin/finatra/explicit/test/route", admin = true) { _: Request =>
              "on the admin interface"
            }

            // routes that start with /admin/finatra ALWAYS added to admin
            get("/admin/finatra/implied/test/roue") { _: Request =>
              response.ok("on the admin interface")
            }
          })
        }
      },
      disableTestLogging = true
    )

    try {
      server.start()

      // A handler should show up on the HttpMuxer
      HttpMuxer.patterns.contains("/admin/finatra/") should be(true)
    } finally {
      server.close()
    }
  }
}
