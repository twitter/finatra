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

* Template support (mustache, jade, scaml via scalate)


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

<dependencies>
  <dependency>
    <groupId>com.posterous</groupId>
    <artifactId>finatra</artifactId>
    <version>4.0.6</version>
  </dependency>
</dependencies>
```

### Configuring

import finatra

```scala
import com.posterous.finatra._
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
import com.posterous.finatra._

object Example extends FinatraApp {

  //render inline html
  get("/") { request =>
    response(body="<h1>asd</h1>")
  }

  // set the status code
  get("/error") { request =>
    response(status=500, body="error!")
  }

  // redirect
  get("/redirector") { request =>
    redirect("/gohere")
  }

  // send json
  get("/somejson") { request =>
    toJson(Map("foo" -> "bar"))
  }

  // setting headers
  get("/headertest") { request =>
    response(body="check headers", headers=Map("X-Foo", "bar"))
  }

  // reading headers
  get("/foo") { request =>
    request.headers.get("X-Foo")
  }

  // will get param /simple?lol=value
  get("/simple") { request =>
    request.params.get("lol")
  }

  // will get /doit/2004
  get("/doit/:year") { request =>
    request.params("year")
  }

  //works the same with, post, delete, head, and put
  post("/file/:id") { request =>
    //handle file here
  }

  delete("/file/:id") { request =>
    //delete the file here
  }

}
```

### File Uploads

```scala
import com.posterous.finatra._

//for multi part support, import the MultipartItem type
import com.capotej.finatra_core.MultipartItem

object UploadExample extends FinatraApp {

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
      case None =>
        response(status=404, body="not found")
    }
  }
}


  //Form Example
  //curl -F foo=bar http://localhost:7070/formsubmit

  post("/formsubmit") { request =>
    request.multiParams("foo").getOrElse(null).data // "bar"
  }


```

## Templating

Finatra provides limited support for the Scalate template engine (for now)

Rendering a template with a variable

```scala

object TemplateExample extends FinatraApp {

   get("/users/:id") { request =>
     request.params.get("id") match {
       case Some(id) =>
         render("users.mustache", Map("user_id" -> id))
     }
   }
}

```

in ```templates/users.mustache```

```mustache
<h1>your user number is {{user_id}}</h1>
```
