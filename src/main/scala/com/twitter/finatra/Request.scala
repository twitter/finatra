package com.twitter.finatra

import scala.collection.mutable.Map
import com.twitter.finatra_core.FinatraRequest

class Request extends FinatraRequest {

  var path: String = "/"
  var method: String = "GET"
  var body: Array[Byte] = Array()
  var params: Map[String, String] = Map()
  var multiParams: Map[String, MultipartItem] = Map()
  var headers: Map[String, String] = Map()
  //var cookies: Map[String, FinatraCookie] = Map()

  def finatraPath   = path
  def finatraMethod = method
  def finatraParams = params

  def path(p:String):Request = {
    this.path = p
    this
  }

  def method(m: String):Request = {
    this.method = m
    this
  }

  def body(b: Array[Byte]):Request = {
    this.body = b
    this
  }

  def params(m: Map[String, String]):Request = {
    this.params = this.params ++ m
    this
  }

  def multiParams(m: Map[String, MultipartItem]):Request = {
    this.multiParams = this.multiParams ++ m
    this
  }

  def headers(m: Map[String, String]):Request = {
    this.headers = this.headers ++ m
    this
  }

  // def cookies(m: Map[String, FinatraCookie]):Request = {
  //   this.cookies = this.cookies ++ m
  //   this
  // }
}
