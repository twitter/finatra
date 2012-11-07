# Finatra [![Build Status](https://secure.travis-ci.org/capotej/finatra.png)](http://travis-ci.org/capotej/finatra)

Finatra is a sinatra-inspired web framework for scala, running on top of [Finagle](http://twitter.github.com/finagle/)

### Notes to users upgrading to 0.3.4

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
  }

  val app = new ExampleApp


  def main(args: Array[String]) = {
    FinatraServer.register(app)
    FinatraServer.start()
  }


```

### Generating your own finatra app

```sh
$ git clone git@github.com:capotej/finatra.git
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
  <version>0.3.4</version>
</dependency>
```

