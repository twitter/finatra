package com.posterous.finatra
import com.codahale.logula.Logging

class FinatraApp extends Logging { 

    def get(path: String)(callback: => Any)    { Router.addRoute("GET", path)(callback) } 
    def delete(path: String)(callback: => Any) { Router.addRoute("DELETE", path)(callback) } 
    def post(path: String)(callback: => Any)   { Router.addRoute("POST", path)(callback) } 
    def put(path: String)(callback: => Any)    { Router.addRoute("PUT", path)(callback) } 
    def head(path: String)(callback: => Any)   { Router.addRoute("HEAD", path)(callback) } 
    def patch(path: String)(callback: => Any)  { Router.addRoute("PATCH", path)(callback) } 

    def params(x:String) = {
      Router.params(x)
    }
}
