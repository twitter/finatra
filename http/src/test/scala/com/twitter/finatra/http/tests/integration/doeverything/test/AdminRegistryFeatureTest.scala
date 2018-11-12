package com.twitter.finatra.http.tests.integration.doeverything.test

import com.twitter.finagle.http.{Method, Request}
import com.twitter.finatra.http.filters.CommonFilters
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.http.tests.integration.doeverything.main.filters.{AppendToHeaderFilter, IdentityFilter}
import com.twitter.finatra.http.{Controller, EmbeddedHttpServer, HttpServer}
import com.twitter.inject.server.FeatureTest
import com.twitter.util.registry.{Entry, GlobalRegistry, SimpleRegistry}

private class AdminRegistryTestController extends Controller {
  // #1
  filter[IdentityFilter]
    .filter[IdentityFilter]
    .filter(new AppendToHeaderFilter("test", "4"))
    .filter[IdentityFilter]
    .get("/multiFilterAppend") { request: Request =>
      request.headerMap("test")
    }

  // #1
  filter(new AppendToHeaderFilter("test", "8"))
    .put("/multiFilterAppend") { request: Request =>
      request.headerMap("test")
    }

  // #2
  filter[IdentityFilter]
    .filter[IdentityFilter]
    .filter[IdentityFilter]
    .get("/multiIdentityFilterAppend") { _: Request =>
      "ok!"
    }

  // #3
  get("/foo") { _: Request =>
    response.ok("foo")
  }

  // #3
  post("/foo") { _: Request =>
    response.ok("foo")
  }

  // #3
  put("/foo") { _: Request =>
    response.ok("foo")
  }

  // #4
  get("/blah") { _: Request =>
    response.ok("blah")
  }

  // #4
  options("/blah") { _: Request =>
    response.ok("blah")
  }

  // #5
  get("/:*") { _: Request =>
    "Hello, world!"
  }

  // #5
  any("/:*") { _: Request =>
    "Hello, any world!"
  }
}

class AdminRegistryFeatureTest extends FeatureTest {
  private[this] val registry = new SimpleRegistry
  private[this] val baseEntryKey = Seq("library", "finatra", "http", "routes")

  override protected def afterAll(): Unit = {
    registry.iterator.foreach(entry => registry.remove(entry.key))
    super.afterAll()
  }

  override val server: EmbeddedHttpServer = GlobalRegistry.withRegistry(registry) {
    val underlying = new EmbeddedHttpServer(
      disableTestLogging = true,
      twitterServer = new HttpServer {
        override protected def configureHttp(router: HttpRouter): Unit = {
          router
            .filter[CommonFilters]
            .add(new AdminRegistryTestController)
        }
      }
    )
    underlying.start()
    underlying
  }

  test("GET /admin/registry.json") {
    verifyEntries(filteredRegistry("/multifilterappend"), "/multifilterappend", Seq(Method.Get, Method.Put))
    verifyEntries(filteredRegistry("/multiidentityfilterappend"), "/multiidentityfilterappend", Seq(Method.Get))
    verifyEntries(filteredRegistry("/foo"), "/foo", Seq(Method.Get, Method.Post, Method.Put))
    verifyEntries(filteredRegistry("/blah"), "/blah", Seq(Method.Get, Method.Options))
    verifyEntries(filteredRegistry("/:*"), "/:*", Seq(Method.Get, Method("ANY")), constant = false)
  }

  private def verifyEntries(entries: Set[Entry], path: String, methods: Seq[Method], constant: Boolean = true): Unit = {
    val details = entries.groupBy(_.key.drop(baseEntryKey.length).mkString("/")).mapValues(_.head.value)
    methods.foreach { method =>
      val base = s"$path/${method.toString.toLowerCase}"
      details(s"$base/constant") should be(constant.toString)
      details(s"$base/method") should be(method.toString)
      details(s"$base/admin") should be("false")
      val actualPath = details(s"$base/path").toLowerCase
      actualPath should be(path)
      if (actualPath.contains(":")) {
        details.contains(s"$base/capture_names") should be(true)
      }

      details(s"$base/callback/request") should be("com.twitter.finagle.http.Request")
      details.get(s"$base/callback/response") should not be None
      details(s"$base/class") should be("com.twitter.finatra.http.tests.integration.doeverything.test.AdminRegistryTestController")
    }
  }

  private[this] def filteredRegistry(route: String): Set[Entry] =
    registry.filter { entry =>
      entry.key.startsWith(baseEntryKey :+ route)
    }.toSet
}
