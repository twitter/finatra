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

<dependencies>
  <dependency>
    <groupId>com.posterous</groupId>
    <artifactId>finatra</artifactId>
    <version>4.1.2</version>
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


  // use rawResponse to send bytes instead of strings
  get("/file") { request =>
    val file = GetTheFile(params.get("file"))
    rawResponse(body=file.toBytes)
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

Use the ```render``` method to render out templates, it takes the path of a template (relative from ```templates/```) and an exported object. Here's an example:

```scala

class UserObject(val id: Int) {
  def displayName = {
    //do your db/service stuff here
    "bob"
  }
}

object TemplateExample extends FinatraApp {
   get("/users/:id") { request =>
     request.params.get("id") match {
       case Some(id) =>
         render(path="users.mustache", exports=new UserObject(id))
     }
   }
}

```

in ```templates/users.mustache```

```mustache
<h1>your user name is {{displayName}} with id of {{id}}</h1>
```

As you can see, all public methods/fields in your exported object can be called from the mustache template.


### Layout

Since ```yield``` is a reserved word in scala, it's called ```render``` in your layouts. An example layout:

```mustache
<html>
  <body>
    {{render}}
  </body>
</html>
```

By default, the layout is ```templates/layouts/application.mustache``` but you can change it by passing another path (relative to ```templates/layouts```) like so:

```scala
object TemplateExample extends FinatraApp {
   get("/users/:id") { request =>
     request.params.get("id") match {
       case Some(id) =>
         render(path="users.mustache", layout="mylayout.mustache", exports= new UserObject(id))
     }
   }
}
```

If the layout is not found, just the template is rendered.

### Layout Functions

If you'd like to be able to call custom functions besides ```render``` in your layout, you'll have to extend the ```LayoutHelperFactory``` and ```LayoutHelper``` classes with your own and set the ```layoutHelperFactory``` to it in ```FinatraServer```. Example:

```scala

import com.posterous.finatra.{FinatraApp, FinatraServer, LayoutHelper, LayoutHelperFactory}

class MyLayoutHelper(yld: String) extends LayoutHelper(yld) {
  val analyticsCode = "UA-5121231"
}

class MyFactory extends LayoutHelperFactory {
  override def apply(str: String) = {
    new MyLayoutHelper(str)
  }
}

FinatraServer.layoutHelperFactory = new MyFactory

```

in they layout you can then do

```mustache
<html>
  <body>
    {{render}}

    <script>
      var code = {{analyticsCode}}
    </script>
    <script src="analytics.js"></script>

  </body>
</html>
```
