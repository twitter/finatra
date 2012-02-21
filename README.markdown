# Finatra

### Description
Finatra is a sinatra clone backed by scala/finagle

### Configuring

import the FinatraServer

```scala
import com.posterous.finatra.FinatraServer
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

