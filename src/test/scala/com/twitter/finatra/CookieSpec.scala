package com.twitter.finatra.test

import com.twitter.finatra.Controller
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import scala.collection.mutable.Map
import org.jboss.netty.handler.codec.http._


class CookieApp extends Controller {

  get("/sendCookie") {
    request => render.plain("get:path").cookie("Foo", "Bar")
  }

  get("/sendAdvCookie") {
    val c = new DefaultCookie("Biz", "Baz")
    c.setSecure(true)
    request => render.plain("get:path").cookie(c)
  }

}

@RunWith(classOf[JUnitRunner])
class CookieSpec extends SpecHelper {

  def app = { new CookieApp }

  "basic k/v cookie" should "have Foo:Bar" in {
    get("/sendCookie")
    response.getHeader("Cookie") should be ("Foo=Bar")
  }

  "advanced Cookie" should "have Biz:Baz&Secure=true" in {
    get("/sendAdvCookie")
    response.getHeader("Cookie") should be ("Biz=Baz;Secure")
  }

}
