package com.twitter.finatra.routing

import com.twitter.finatra.internal.routing.PathPattern
import com.twitter.inject.Test
import java.net.URI

class PathPatternTest extends Test {

  "path pattern" should {
    "routes" in {
      PathPattern("/cars").extract("/cars") should equal(Some(Map()))
      PathPattern("/cars").extract("/cars/") should equal(None)

      PathPattern("/cars/").extract("/cars") should equal(None)
      PathPattern("/cars/").extract("/cars/") should equal(Some(Map()))

      PathPattern("/cars/?").extract("/cars") should equal(Some(Map()))
      PathPattern("/cars/?").extract("/cars/") should equal(Some(Map()))

      PathPattern("/cars:*").extract("/cars") should equal(Some(Map("*" -> "")))
      PathPattern("/cars:*").extract("/cars/") should equal(Some(Map("*" -> "/")))

      PathPattern("/cars/:id").extract("/cars/123") should equal(Some(Map("id" -> "123")))
      PathPattern("/cars/:id").extract("/cars/") should equal(None)

      PathPattern("/store/cars/:id").extract("/store/cars/123") should equal(Some(Map("id" -> "123")))
      PathPattern("/store/cars/:id").extract("/asdf/cars/123") should equal(None)

      PathPattern("/cars/:make/:model").extract("/cars/ford/explorer") should equal(Some(Map("make" -> "ford", "model" -> "explorer")))
      PathPattern("/cars/:make/:model").extract("/cars/foo/ford/explorer") should equal(None)

      PathPattern("/cars/:make/:model").extract("/cars/1-1/2") should equal(Some(Map("make" -> "1-1", "model" -> "2")))
      PathPattern("/cars/:make/:model").extract("/cars/ford/") should equal(None)
      PathPattern("/cars/:make/:model").extract("/cars/ford") should equal(None)

      PathPattern("/store/cars/:make/:model").extract("/store/cars/ford/explorer") should equal(Some(Map("make" -> "ford", "model" -> "explorer")))
      PathPattern("/cars/:make/:model/.*").extract("/cars/ford/explorer/foo/bar") should equal(Some(Map("make" -> "ford", "model" -> "explorer")))
    }

    "non capture group syntax" in {
      PathPattern("/(?:cars|boats)/:id").extract("/cars/123") should equal(Some(Map("id" -> "123")))
      PathPattern("/(?:cars|boats)/:id").extract("/boats/123") should equal(Some(Map("id" -> "123")))
    }

    "capture group syntax is an assertion error" in {
      intercept[AssertionError] {
        PathPattern("/(cars|boats)/:id")
      }
    }

    "routes w/ special '*' token" in {
      PathPattern("/:*").extract("/") should equal(Some(Map("*" -> "")))
      PathPattern("/:*").extract("/abc") should equal(Some(Map("*" -> "abc")))
      PathPattern("/:*").extract("/abc/123") should equal(Some(Map("*" -> "abc/123")))

      PathPattern("/ui/:*").extract("/ui/") should equal(Some(Map("*" -> "")))
      PathPattern("/ui/:*").extract("/ui/abc") should equal(Some(Map("*" -> "abc")))
      PathPattern("/ui/:*").extract("/ui/abc/123") should equal(Some(Map("*" -> "abc/123")))
    }

    "unicode" in {
      pending
      val path = "위키백과"
      val escapedUri = "/" + new URI(path).toASCIIString
      PathPattern("/" + path).extract(escapedUri) should equal(Some())
    }
  }
}
