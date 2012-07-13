package com.twitter.finatra

import com.twitter.finagle.builder.{Server, ServerBuilder}
import com.twitter.finagle.http.Http
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finatra_core.{AbstractFinatraController, ControllerCollection}
import com.twitter.logging.config._
import com.twitter.logging.Logger
import com.twitter.util.Future
import java.net.InetSocketAddress
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse}

object FinatraServer extends Logging {

  val controllers = new ControllerCollection[Request, Response, Future[HttpResponse]]
  var docroot = "public"

  val config = new LoggerConfig {
    node = ""
    level = Logger.INFO
    handlers = new FileHandlerConfig {
      filename = "logs/finatra.log"
      roll = Policy.Daily
    }
  }
  config()

  def register(app: AbstractFinatraController[Request, Response, Future[HttpResponse]]) { controllers.add(app) }

  def start(port:Int = 7070, docroot:String = "public") {
    this.docroot = docroot

    val appService = new AppService
    val fileService = new FileService

    val service: Service[HttpRequest, HttpResponse] = fileService andThen appService

    val server: Server = ServerBuilder()
      .codec(Http())
      .bindTo(new InetSocketAddress(port))
      .name("finatraServer")
      .build(service)

    logger.info("started on %s, docroot %s", port, docroot)
    println("started on 7070: view logs/finatra.log for more info")
  }

}

