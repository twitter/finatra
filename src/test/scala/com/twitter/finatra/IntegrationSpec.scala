package com.twitter.finatra.test

import com.twitter.finatra.Controller
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import scala.collection.mutable.Map

class MyApp extends Controller {
  get("/path")    { request => response("get:path") }
  post("/path")   { request => response("post:path") }
  put("/path")    { request => response("put:path") }
  delete("/path") { request => response("delete:path") }
  patch("/path")  { request => response("patch:path") }
  get("/params")  { request => response(request.params("p")) }
  get("/headers") { request => response(request.headers("Referer")) }
}

@RunWith(classOf[JUnitRunner])
class IntegrationSpec extends SpecHelper {

  def app = { new MyApp }

  "GET /path" should "respond 200" in {
    get("/path")
    response should equal ("get:path")
  }

  "POST /path" should "respond 200" in {
    post("/path")
    response should equal ("post:path")
  }

  "PUT /path" should "respond 200" in {
    put("/path")
    response should equal ("put:path")
  }

  "DELETE /path" should "respond 200" in {
    delete("/path")
    response should equal ("delete:path")
  }

  "PATCH /path" should "respond 200" in {
    patch("/path")
    response should equal ("patch:path")
  }

  "GET /params" should "respond 200" in {
    get("/params", Map("p"->"yup"))
    response should equal ("yup")
  }

  "GET /headers" should "respond 200" in {
    get("/headers", headers=Map("Referer"->"http://twitter.com"))
    response should equal ("http://twitter.com")
  }

}
