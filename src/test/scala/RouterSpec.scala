package com.posterous.finatra 

import org.junit.Test
import com.codahale.simplespec.Spec

 
class RouterSpec extends Spec {

  class `route hashing` {

    @Test def `should store a route` = {
      Router.addRoute("GET", "/") { 2 + 2 } 
      Router.routes.get("GET/").getOrElse(null)().must(be(4))
    }

    @Test def `should return the method and path together` = {
      Router.hashKey("GET", "/").must(be("GET/"))
    }
  }


}

