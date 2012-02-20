package com.posterous.finatra

object Whatever extends FinatraApp("/my") {
  get("/lol") { <h1>lol</h1> }
}


object SomeShit extends FinatraApp {

  get("/") { 
    mediaType("application/html")
    "<h1>asd</h1>"
  } 

  get("/error") {
    status(500)
    "error!"
  }

  get("/headertest") {
    headers("foo","bar")
    "check heads"
  }

  get("/foo") { 
    request.headers 
  }
  
  get("/simple") { 
    params("lol") 
  }
  
  get("/doit/:year") { 
    params("year") 
  }

}

