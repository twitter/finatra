# Finatra

### Description
Finatra is a sinatra clone backed by scala/finagle

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
    <version>1.2.4</version>
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
FinatraServer.start() //Default is port 7070, pass Int here to change
```

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
## Credits
Julio Capote @capotej

Christopher Burnett @twoism
