package com.twitter.finatra.http.tests.integration.routing

import com.twitter.finagle.http.{HttpMuxer, Request}
import com.twitter.finatra.http.routing.{AdminIndexInfo, HttpRouter}
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.finatra.http.{Controller, HttpServer}
import com.twitter.inject.Test

class HttpServerAdminRouteTest extends Test {

  "Server" should {

    "not allow conflicting admin routes" in {
      val server =  new EmbeddedHttpServer(
        twitterServer = new HttpServer {
          override protected def configureHttp(router: HttpRouter) = {
            router.add(new Controller {
              get("/healthy", name = "Health", admin = true) { request: Request =>
                response.ok("OK\n")
              }

              get("/quitquitquit", name = "Quit", admin = true) { request: Request =>
                response.ok("go away")
              }

              post("/abortabortabort", admin = true) { request: Request =>
                response.ok("go away now")
              }
            })
          }
        })

      try {
        // defines overlapping admin route paths
        intercept[java.lang.AssertionError] {
          server.start()
        }
      } finally {
        server.close()
      }
    }

     "not allow conflicting admin routes that start with /admin/" in {
      val server =  new EmbeddedHttpServer(
        twitterServer = new HttpServer {
          override protected def configureHttp(router: HttpRouter) = {
            router.add(new Controller {

              get("/admin/tracing", admin = true) { request: Request =>
                response.ok("trace\n")
              }

              get("/admin/server_info", admin = true) { request: Request =>
                response.ok("some info here\n")
              }
            })
          }
        })

      try {
        // defines overlapping admin route paths
        intercept[Exception] {
          server.start()
        }
      } finally {
        server.close()
      }
    }

    "properly add routes to admin index" in {
      val server =  new EmbeddedHttpServer(
        twitterServer = new HttpServer {
          override protected def configureHttp(router: HttpRouter) = {
            router.add(new Controller {
              get("/admin/finatra/stuff/:id",
                admin = true,
                adminIndexInfo = Some(AdminIndexInfo())) { request : Request =>
                response.ok.json(request.params("id"))
              }

              get("/admin/finatra/foo",
                adminIndexInfo = Some(AdminIndexInfo())) { request: Request =>
                response.ok.json("bar")
              }

              post("/admin/resource", admin = true) { request: Request =>
                response.ok("affirmative")
              }

              get("/admin/thisisacustompath",
                admin = true,
                adminIndexInfo = Some(
                  AdminIndexInfo(
                    alias = "Custom",
                    group = "My Service"))) { request: Request =>
                response.ok.json("pong")
              }

              post("/admin/finatra/ok/computer",
                admin = true,
                adminIndexInfo = Some(AdminIndexInfo())) { request : Request =>
                response.ok("roger.")
              }
            })
          }
        })

      try {
        server.start()

        // GET /admin/finatra/stuff/:id non-constant route should not be added
        server.adminHttpServerRoutes.map(_.path).contains("/admin/finatra/stuff/:id") should be(false)

        // POST /admin/finatra/ok/computer constant route should not be added
        server.adminHttpServerRoutes.map(_.path).contains("/admin/finatra/ok/computer") should be(false)

        // GET /admin/finatra/foo constant route should not be added
        server.adminHttpServerRoutes.map(_.path).contains("/admin/finatra/foo") should be(false)

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

        // A handler should show up on the HttpMuxer
        HttpMuxer.patterns.contains("/admin/finatra/") should be(true)
      } finally {
        server.close()
      }
    }

    "Add HttpMuxer handler for /admin/finatra and non-constant or non-GET routes" in {
      // if we only add non-constant routes to the admin there should still be a handler
      // registered on the HttpMuxer
      val server =  new EmbeddedHttpServer(
        twitterServer = new HttpServer {
          override protected def configureHttp(router: HttpRouter) = {
            router.add(new Controller {
              get("/admin/finatra/prefix/resource/:id") { request: Request =>
                response.ok.json(request.params("id"))
              }
            })
          }
        })

      try {
        server.start()

        // A handler should show up on the HttpMuxer
        HttpMuxer.patterns.contains("/admin/finatra/") should be(true)
      } finally {
        server.close()
      }
    }
  }
}
