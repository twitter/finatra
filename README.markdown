# Finatra [![Build Status](https://secure.travis-ci.org/capotej/finatra.png)](http://travis-ci.org/capotej/finatra)

### Description
Finatra is a sinatra clone backed by scala/finagle

### API

```scala
class HelloWorld extends Controller {

  def tweets = List(new Tweet("hey!"), new Tweet("lol"))

  get("/plain") { request =>
    render.plain("hello world").toFuture
  }

  get("/cookies") { request =>
    render.json(request.cookies).header("Content-Type", "text/html").toFuture
  }

  get("/tweets.json") { request =>
    render.json(tweets).toFuture
  }

  get("/status/:status") { request =>
    val statusCode = request.params("status").toInt

    render.nothing.status(statusCode).toFuture
  }

  get("/not_found") { request =>
    render.nothing.notFound.toFuture
  }

  get("/headers") { request =>
    render.nothing.header("X-GitSHA", "1ecd6b1").toFuture
  }

}
```

### Features
* The routing DSL you've come to know and love

* Asynchronous, uses Finagle-HTTP/Netty

* Multipart file upload/form handling

* Modular app support

* A testing helper

* Built in static file server (note: not designed for huge files(>100mb))

* Mustache template support through [mustache.java](https://github.com/spullara/mustache.java)


### Installation via RubyGems

    $ gem install finatra

    $ finatra new MyApp
    Org Name (com.<username>) com.twitter
    [finatra] :: Creating MyApp in /Users/cburnett/dev...
      create  myapp
      create  myapp/Procfile
      create  myapp/README.markdown
      create  myapp/pom.xml
      create  myapp/src/main/scala/com/twitter/myapp/MyApp.scala
      create  myapp/src/test/scala/com/twitter/myapp/MyAppSpec.scala
      create  myapp/src/main/resources/timeline.mustache

### Start the Server

    $ finatra start
    [INFO] Scanning for projects...
    [INFO]
    [INFO] ------------------------------------------------------------------------
    [INFO] Building MyApp 0.0.1-SNAPSHOT
    [INFO] ------------------------------------------------------------------------
    [INFO] No known dependencies. Compiling everything
    [INFO] launcher 'main' selected => com.twitter.myapp.App
    started on 7070: view logs/finatra.log for more info

### Other Commands

    $ finatra
    Tasks:
      finatra compile          # Compile your app with Maven
      finatra console          # Starts a scala console
      finatra help [TASK]      # Describe available tasks or one specific task
      finatra new <MyAppName>  # Create a new Finatra app
      finatra package          # Package a jar for your app
      finatra start            # Run your app on port 7070
      finatra test             # Run the tests

### Installation via Maven
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
  <version>0.3.2</version>
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

### Views

Template files are found in `src/main/resources` by default. You can change this location by setting `baseTemplatePath`.

```scala
View.baseTemplatePath = "/templates"
```

Finatra views currently only support Mustache templates and are defined as follows.

#### timeline.mustache
```mustache
{{#tweets}}
  {{status}}
{{/tweets}}
```

```scala
case class Tweet(status:String)

class TimelineView(val tweets:List[Tweet]) extends View {
  val template = "timeline.mustache"
}

class HelloWorld extends Controller {

  def tweets = List(new Tweet("hey!"), new Tweet("lol"))

  get("/tweets") { request =>
    val tweetsView  = new TimelineView(tweets)

    render.view(tweetsView).toFuture
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

        render.ok.toFuture
      case None =>
        render.notFound.toFuture
    }
  }
}

```
