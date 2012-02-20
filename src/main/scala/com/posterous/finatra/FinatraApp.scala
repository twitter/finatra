package com.posterous.finatra
import com.codahale.logula.Logging


class FinatraApp(var prefix: String = "") extends Logging { 
     
    def get(path: String)(callback: => Any)    { Router.addRoute("GET", prefix + path)(callback) } 
    def delete(path: String)(callback: => Any) { Router.addRoute("DELETE", prefix + path)(callback) } 
    def post(path: String)(callback: => Any)   { Router.addRoute("POST", prefix + path)(callback) } 
    def put(path: String)(callback: => Any)    { Router.addRoute("PUT", prefix + path)(callback) } 
    def head(path: String)(callback: => Any)   { Router.addRoute("HEAD", prefix + path)(callback) } 
    def patch(path: String)(callback: => Any)  { Router.addRoute("PATCH", prefix + path)(callback) } 

    def params(name:String) = { Router.params(name) }
    def response() = { Router.response }
    def request() = { Router.request }
    def headers(pair:Tuple2[String,String]) = { response.headers += pair }
    def status(code:Int) = { response.status = code }
    def contentType(mtype:String) = { response.mediaType = mtype }
}
