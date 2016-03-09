package com.twitter.finatra.http.integration.routing

import com.twitter.finagle.http.{HttpMuxer, Request}
import com.twitter.finatra.http.routing.{AdminIndexInfo, HttpRouter}
import com.twitter.finatra.http.test.EmbeddedHttpServer
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

        // can't add non-constant routes to the admin routes
        server.adminHttpServerRoutes.map(_.path).contains("/admin/finatra/stuff/:id") should be(false)

        // can't add POST route even if it's a constant route
        server.adminHttpServerRoutes.map(_.path).contains("/admin/finatra/ok/computer") should be(false)

        // /admin/thisisacustompath constant route should be added
        val adminCustomRoute = server.adminHttpServerRoutes.find(_.path == "/admin/thisisacustompath")
        adminCustomRoute.isDefined should be(true)
        adminCustomRoute.get.group should be(Some("My Service"))
        adminCustomRoute.get.alias should be("Custom")

        // /admin/finatra/foo constant route should be added
        val adminFinatraRoute = server.adminHttpServerRoutes.find(_.path == "/admin/finatra/foo")
        adminFinatraRoute.isDefined should be(true)
        adminFinatraRoute.get.group should be(Some("Finatra")) // default
        adminFinatraRoute.get.alias should be(adminFinatraRoute.get.path) // default is path

        // A handler should show up on the HttpMuxer
        HttpMuxer.patterns.contains("/admin/finatra/") should be(true)
      } finally {
        server.close()
      }
    }

    "Add HttpMuxer handler for non-constant routes" in {
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