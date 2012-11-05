# Finatra [![Build Status](https://secure.travis-ci.org/capotej/finatra.png)](http://travis-ci.org/capotej/finatra)

### Description
Finatra is a sinatra clone backed by scala/finagle

### API

```scala


  class ExampleApp extends Controller {
    get("/") { request =>
      render.plain("ok").toFuture
    }
  }

  val app = new ExampleApp


```

### Features
* The routing DSL you've come to know and love

* Asynchronous, uses Finagle-HTTP/Netty

* Multipart file upload/form handling

* Modular app support

* A testing helper

* Built in static file server (note: not designed for huge files(>100mb))

* Mustache template support through [mustache.java](https://github.com/spullara/mustache.java)

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
  <version>###VERSION###</version>
</dependency>
```

### Example project
Check out [finatra-example](http://github.com/capotej/finatra-example) for an example finatra project

Here's an [example blog](https://gist.github.com/2626200)

