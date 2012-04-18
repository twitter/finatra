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
    <version>2.0.0</version>
  </dependency>
</dependencies>
```

### Configuring

import the FinatraServer and FinatraApp

```scala
import com.posterous.finatra.{FinatraServer, FinatraApp}
```

register your apps

```scala
FinatraServer.register(Example)
FinatraServer.register(MyResource)
```

start the server

```scala
FinatraServer.start() //Defaults: 7070, "docroot"
```


### Example project
Check out [finatra-helloworld](http://github.com/capotej/finatra-helloworld) for an example finatra project


### API

```scala
package com.posterous.finatra

//You can "mount" your resources on a prefix
object MyResource extends FinatraApp("/my") {
  // responds to /my/lol
  get("/lol") { <h1>lol</h1> }
}

object Example extends FinatraApp {

  // set content type
  get("/") { 
    contentType("application/html")
    "<h1>asd</h1>"
  } 

  // set the status code
  get("/error") {
    status(500)
    "error!"
  }

  // redirect 
  get("/redirector") {
    redirect("/gohere")
  }
  
  // send json
  get("/somejson") {
    toJson(Map("foo" -> "bar"))
  }
  
  // setting headers
  get("/headertest") {
    headers("foo","bar")
    "check heads"
  }

  // reading headers
  get("/foo") { 
    request.headers 
  }
 
  // will get param /simple?lol=value
  get("/simple") { 
    params("lol") 
  }
 
  // will get /doit/2004
  get("/doit/:year") { 
    params("year") 
  }

  // respond to post
  post("/file") {
    "file posted"
  }

}
```

### File Uploads

```scala

object UploadExample extends FinatraApp {

  //Example curl:
  //curl -F myfile=@/home/capotej/images/bad-advice-cat.jpeg http://localhost:7070/

  //the multiPart method returns MultiPartItem objects, which have some handy methods
  post("/upload") {
    multiPart("myfile").headers 
    
    multiPart("myfile").contentType
    
    multiPart("myfile").data
    
    multiPart("myfile").filename
    
    multiPart("myfile").writeToFile("/tmp/file.jpg")
  }


  //Form Example
  //curl -F foo=bar http://localhost:7070/formsubmit

  post("/formsubmit") {
    multiPart("foo").data // "bar"
  }


```

## Templating

Finatra provides limited support for the Scalate template engine (for now)

Rendering a template with a variable

```scala

object TemplateExample extends FinatraApp {
   
   get("/users/:id") { 
     "user_id" << params("id")
     render("users.mustache") 
   }

}

```

in ```templates/users.mustache```

```mustache
<h1>your user number is {{user_id}}</h1>
```

## Writing tests
Finatra includes FinatraSpec for easy test writing

```scala
import com.posterous.finatra.FinatraSpec

class IntHandlerSpec extends FinatraSpec {

  FinatraServer.register(IntHandler)

  class `GET for a missing key'` {

    get("/int/foo")

    @Test def `returns 404` = {
      lastResponse.statusCode.must(be(404))
    }

  }

  class `POST for a missing key, creates it with 1'` {

    post("/int/foo")
    get("/int/foo")

    @Test def `returns 200` = {
      lastResponse.statusCode.must(be(200))
      lastResponse.content.toString("UTF8").must(be("1"))
    }

  }
  class `POST for an existing key, with a value of 10'` {

    post("/int/foo2", List(Tuple2("value", "10")):_*)
    get("/int/foo2")

    @Test def `returns 200` = {
      lastResponse.statusCode.must(be(200))
      lastResponse.content.toString("UTF8").must(be("10"))
    }

  }
}
```

## License 

Copyright (C) 2012 Julio Capote

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
