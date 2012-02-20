package com.posterous.finatra

object Whatever extends FinatraApp("/my") {
  get("/lol") { <h1>lol</h1> }
}


object SomeShit extends FinatraApp {

  get("/") { <h1>asd</h1> } 

  post("/foo") { <h1>foo</h1> }
  
  get("/simple") { params("lol") }
  
  get("/doit/:year") { params("year") }

}

