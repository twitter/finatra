package com.twitter.finatra.test

import com.twitter.finatra.Controller
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import scala.collection.mutable.Map

class MyApp extends Controller {
  get("/path")    { request => render.plain("get:path") }
  post("/path")   { request => render.plain("post:path") }
  put("/path")    { request => render.plain("put:path") }
  delete("/path") { request => render.plain("delete:path") }
  patch("/path")  { request => render.plain("patch:path") }
  get("/params")  { request => render.plain(request.params("p")) }
  get("/headers") { request => render.plain(request.headers("Referer")) }
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
