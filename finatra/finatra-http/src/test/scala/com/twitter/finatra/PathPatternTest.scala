package com.twitter.finatra

import com.twitter.finatra.test.Test
import com.twitter.finatra.twitterserver.routing.PathPattern
import com.twitter.util.Stopwatch
import java.util.concurrent.TimeUnit._

class PathPatternTest extends Test {

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

  "routes w/ special '*' token" in {
    PathPattern("/:*").extract("/") should equal(Some(Map("*" -> "")))
    PathPattern("/:*").extract("/abc") should equal(Some(Map("*" -> "abc")))
    PathPattern("/:*").extract("/abc/123") should equal(Some(Map("*" -> "abc/123")))

    PathPattern("/ui/:*").extract("/ui/") should equal(Some(Map("*" -> "")))
    PathPattern("/ui/:*").extract("/ui/abc") should equal(Some(Map("*" -> "abc")))
    PathPattern("/ui/:*").extract("/ui/abc/123") should equal(Some(Map("*" -> "abc/123")))
  }

  //TODO: Move to finatra-benchmarks
  "perf test" in {
    val pathPattern = PathPattern("/store/cars/:make/:model")
    val total = 1000000
    run(total, pathPattern)
    run(total, pathPattern)
    run(total, pathPattern)
    run(total, pathPattern)
  }

  def run(total: Int, pathPattern: PathPattern) {
    val stopwatch = Stopwatch.start()
    for (i <- 1 to total) {
      pathPattern.extract("/store/cars/ford/explorer")
    }
    val millis = stopwatch().inUnit(MILLISECONDS)
    println("Total: " + millis + "ms")
    println("Each: " + millis / total + "ms")
    println()
  }
}
