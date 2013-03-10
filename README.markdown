

# Finatra [![Build Status](https://secure.travis-ci.org/capotej/finatra.png)](http://travis-ci.org/capotej/finatra)

Finatra is a sinatra-inspired web framework for scala, running on top of [Finagle](http://twitter.github.com/finagle/)

### Notes to users upgrading to 1.0.0 from earlier versions

* route parameters now live in ```request.multiParams```

* public assets now live in ```src/main/resources``` (can be configured, see below)


### Features

* Familiar routing DSL

* Asynchronous, uses [Finagle](http://twitter.github.com/finagle/)

* Multipart Upload

* File server with live asset reloading

* App Generator

* Mustache template support through [mustache.java](https://github.com/spullara/mustache.java)

* Heroku support out of the box

### Projects using finatra

[Zipkin](http://twitter.github.com/zipkin/) is an awesome distributed tracing system

[finatra-example](http://github.com/capotej/finatra-example) An example repo to get you started


### Example

```scala

object App {

  
  class ExampleApp extends Controller {

    /**
     * Basic Example
     *
     * curl http://localhost:7070/hello => "hello world"
     */
    get("/") { request =>
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


    /**
     * Custom Error Handling
     *
     * curl http://localhost:7070/error
     */
    get("/error")   { request =>
      1234/0
      render.plain("we never make it here").toFuture
    }

    /**
     * Custom Error Handling with custom Exception
     *
     * curl http://localhost:7070/unautorized
     */
    class Unauthorized extends Exception

    get("/unauthorized") { request =>
      throw new Unauthorized
    }

    error { request =>
      request.error match {
        case Some(e:ArithmeticException) =>
          render.status(500).plain("whoops, divide by zero!").toFuture
        case Some(e:Unauthorized) =>
          render.status(401).plain("Not Authorized!").toFuture
        case Some(e:UnsupportedMediaType) =>
          render.status(415).plain("Unsupported Media Type!").toFuture
        case _ =>
          render.status(500).plain("Something went wrong!").toFuture
      }
    }


    /**
     * Custom 404s
     *
     * curl http://localhost:7070/notfound
     */
    notFound { request =>
      render.status(404).plain("not found yo").toFuture
    }


    /**
     * Dispatch based on Content-Type
     *
     * curl http://localhost:7070/index.json
     * curl http://localhost:7070/index.html
     */
    get("/blog/index.:format") { request =>
      respondTo(request) {
        case _:Html => render.html("<h1>Hello</h1>").toFuture
        case _:Json => render.json(Map("value" -> "hello")).toFuture
      }
    }

    /**
     * Also works without :format route using browser Accept header
     *
     * curl -H "Accept: text/html" http://localhost:7070/another/page
     * curl -H "Accept: application/json" http://localhost:7070/another/page
     * curl -H "Accept: foo/bar" http://localhost:7070/another/page
     */

    get("/another/page") { request =>
      respondTo(request) {
        case _:Html => render.plain("an html response").toFuture
        case _:Json => render.plain("an json response").toFuture
        case _:All => render.plain("default fallback response").toFuture
      }
    }

    /**
     * Metrics are supported out of the box via Twitter's Ostrich library.
     * More details here: https://github.com/twitter/ostrich
     *
     * curl http://localhost:7070/slow_thing
     *
     * By default a stats server is started on 9990:
     *
     * curl http://localhost:9990/stats.txt
     *
     */

    get("/slow_thing") { request =>
      Stats.incr("slow_thing")
      Stats.time("slow_thing time") {
        Thread.sleep(100)
      }
      render.plain("slow").toFuture
    }

  }

  val app = new ExampleApp

  def main(args: Array[String]) = {
    FinatraServer.register(app)
    FinatraServer.start()
  }


```

### Generating your own finatra app

```sh
$ git clone https://github.com/capotej/finatra.git
$ cd finatra
$ ./finatra new com.example.myapp /tmp
```

That will generate ```/tmp/myapp/```, start it up like so:

```sh
$ cd /tmp/myapp
$ mvn scala:run
```

You should now have finatra running on port 7070!

### Installing the generator

For bash users:

    echo 'eval "$(./finatra init -)"' >> ~/.bash_profile
    exec bash

For zsh users:

    echo 'eval "$(./finatra init -)"' >> ~/.zshenv
    source ~/.zshenv


Now you can run ```finatra new``` from anywhere.

### Configuration

Available configuration properties and their defaults

```sh
-Dname=finatra
-Dlog_path=logs/finatra.log
-Dlog_node=finatra
-Dport=7070
-Dmax_request_megabytes=5
-Dstats_enabled=true
-Dstats_port=9990
-Dlocal_docroot=src/main/resources
-Dpid_enabled=false
-Dpid_path=finatra.pid
-Denv=development
```

### Installation via Maven
Add the dependency to your pom.xml

```xml
<dependency>
  <groupId>com.twitter</groupId>
  <artifactId>finatra</artifactId>
  <version>1.3.0</version>
</dependency>
```

