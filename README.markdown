# Finatra

### Description
Finatra is a sinatra clone backed by scala/finagle written by [@capotej](http://twitter.com/capotej) and [@twoism](http://twitter.com/twoism)


### Features
* The routing DSL you've come to know and love

* Asynchronous, uses Finagle-HTTP/Netty

* Multipart file upload/form handling

* Modular app support

* A testing helper

* Built in static file server (note: not designed for huge files(>100mb))

* Mustache template support through [mustache.java](https://github.com/spullara/mustache.java)

### TODO
* Make file serving more efficient / Use an LRU map

* Plugin api


### Installation
Add the repo and dependency to your pom.xml (sbt users to the left)

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
  <version>0.1.7</version>
</dependency>
```

### Configuring

import finatra

```scala
import com.twitter.finatra.{FinatraServer, Controller, View}
```

add your apps
```scala
val exampleApp = new ExampleApp
val myResource = new MyResource

FinatraServer.register(exampleApp)
FinatraServer.register(myResource)
```

start the server

```scala
FinatraServer.start() //Defaults: 7070, "docroot"
```


### Example project
Check out [finatra-helloworld](http://github.com/capotej/finatra-helloworld) for an example finatra project

Here's an [example blog](https://gist.github.com/2626200)


### API

```scala
class HelloWorld extends Controller {

  def tweets = List(new Tweet("hey!"), new Tweet("lol"))

  get("/cookies") { request =>
    render.json(request.cookies).header("Content-Type", "text/html")
  }

  get("/tweets.json") { request =>
    render.json(tweets)
  }

  get("/status/:status") { request =>
    val statusCode = request.params("status").toInt

    render.nothing.status(statusCode)
  }

  get("/not_found") { request =>
    render.nothing.notFound
  }

  get("/headers") { request =>
    render.nothing.header("X-GitSHA", "1ecd6b1")
  }

}
```

### Views

```scala

class TimelineView(val tweets:List[Tweet]) extends View {
  val template = "timeline.mustache"
}

class HelloWorld extends Controller {

  def tweets = List(new Tweet("hey!"), new Tweet("lol"))

  get("/tweets") { request =>
    val tweetsView  = new TimelineView(tweets)

    render.view(tweetsView)
  }

}
```

### File Uploads

```scala
import com.twitter.finatra._

object UploadExample extends Controller {

  //Example curl:
  //curl -F myfile=@/home/capotej/images/bad-advice-cat.jpeg http://localhost:7070/upload

  //the multiPart method returns MultiPartItem objects, which have some handy methods
  post("/upload") { request =>

    request.multiParams.get("myfile") match {
      case Some(file) =>

        //get the content type
        file.contentType

        //get the data
        file.data

        //get the uploaded filename
        file.filename

        //copy the file somewhere
        file.writeToFile("/tmp/uploadedfile.jpg")
        render.ok
      case None =>
        render.notFound
    }
  }
}


  //Form Example
  //curl -F foo=bar http://localhost:7070/formsubmit

  post("/formsubmit") { request =>
    request.multiParams("foo").getOrElse(null).data // "bar"
  }


```
