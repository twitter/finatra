# Finatra [![Build Status](https://secure.travis-ci.org/capotej/finatra.png)](http://travis-ci.org/capotej/finatra)

### Features

* Familiar routing DSL

* Asynchronous, uses Finagle/Netty

* Multipart Upload

* File server with live asset reloading

* App Generator

* Mustache template support through [mustache.java](https://github.com/spullara/mustache.java)

### Example

```scala

object App {

  
  class ExampleApp extends Controller {

    /**
     * Basic Example
     *
     * curl http://localhost:7070/hello => "hello world"
     */
    get("/hello") { request =>
      render.plain("hello world").toFuture
    }

    /**
     * Route parameters
     *
     * curl http://localhost:7070/user/dave => "hello dave"
     */
    get("/user/:username") { request =>
      val username = request.routeParams.getOrElse("username", "default_user")
      render.plain("hello " + username).toFuture
    }

    /**
     * Setting Headers
     *
     * curl -I http://localhost:7070/headers => "Foo:Bar"
     */
    get("/headers") { request =>
      render.plain("look at headers").header("Foo", "Bar").toFuture
    }

    /**
     * Rendering json
     *
     * curl -I http://localhost:7070/headers => "Foo:Bar"
     */
    get("/data.json") { request =>
      render.json(Map("foo" -> "bar")).toFuture
    }

    /**
     * Query params
     *
     * curl http://localhost:7070/search?q=foo => "no results for foo"
     */
    get("/search") { request =>
      request.params.get("q") match {
        case Some(q) => render.plain("no results for "+ q).toFuture
        case None    => render.plain("query param q needed").status(500).toFuture
      }
    }

    /**
     * Uploading files
     *
     * curl -F avatar=@/path/to/img http://localhost:7070/profile
     */
    post("/profile") { request =>
      request.multiParams.get("avatar").map { avatar =>
        println("content type is " + avatar.contentType)
        avatar.writeToFile("/tmp/avatar") //writes uploaded avatar to /tmp/avatar
      }
      render.plain("ok").toFuture
    }

    /**
     * Rendering views
     *
     * curl http://localhost:7070/posts
     */
    class AnView extends View {
      val template = "an_view.mustache"
      val some_val = "random value here"
    }

    get("/template") { request =>
      val anView = new AnView
      render.view(anView).toFuture
    }
  }

  val app = new ExampleApp


  def main(args: Array[String]) = {
    FinatraServer.register(app)
    FinatraServer.start()
  }


```


### Configuration

The available configuration properties and their defaults

```sh
-Dname=finatra
-Dlog_path=logs/finatra.log
-Dlog_node=finatra
-Dport=7070
-Dlocal_docroot=src/main/resources
-Dpid_enabled=false
-Dpid_path=finatra.pid
-Denv=development
```


### Installation via Maven
Add the repo and dependency to your pom.xml

```xml
<repositories>
  <repository>
    <id>repo.juliocapote.com</id>
    <url>http://repo.juliocapote.com</url>
  </repository>
</repositories>

<dependency>
  <groupId>com.twitter</groupId>
  <artifactId>finatra</artifactId>
  <version>0.3.4-SNAPSHOT</version>
</dependency>
```

### Example project
Check out [finatra-example](http://github.com/capotej/finatra-example) for an example finatra project

Here's an [example blog](https://gist.github.com/2626200)

