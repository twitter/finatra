# Finatra

### Description
Finatra is a sinatra clone backed by scala/finagle


### Current api/features

```scala
package com.posterous.finatra

//You can "mount" your resources on a prefix
object Whatever extends FinatraApp("/my") {
  get("/lol") { <h1>lol</h1> }
}

object MyResource extends FinatraApp {

  // set media type
  get("/") { 
    response.mediaType = "application/html"
    "<h1>asd</h1>"
  } 

  // set the status code
  get("/error") {
    response.status = 500
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

}
```

