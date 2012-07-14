package com.twitter.finatra

import com.twitter.finagle.builder.{Server, ServerBuilder}
import com.twitter.finagle.http.Http
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finatra_core.{AbstractFinatraController, ControllerCollection}
import com.twitter.logging.config._
import com.twitter.logging.{Logger, LoggerFactory, FileHandler}
import com.twitter.util.Future
import java.io.{File, FileOutputStream}
import java.lang.management.ManagementFactory
import java.net.InetSocketAddress
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse}

object FinatraServer extends Logging {

  val controllers = new ControllerCollection[Request, Response, Future[HttpResponse]]

  var docroot = "public"
  var pidPath = "finatra.pid"

  val logHandler = FileHandler(filename = "logs/finatra.org", rollPolicy = Policy.Daily)
  val config = new LoggerFactory(node = "finatra",
                                 level = Some(Logger.INFO),
                                 handlers = List(logHandler))()

  def register(app: AbstractFinatraController[Request, Response, Future[HttpResponse]]) { controllers.add(app) }

  def shutdown = {
    logger.info("shutting down")
    println("shutting down")
    new File(pidPath).delete
  }

  def start(basePort:Int = 7070, docroot:String = "public", pidPath:String = "finatra.pid") {
    this.docroot = docroot
    this.pidPath = pidPath

    val appService = new AppService
    val fileService = new FileService

    val envPort = Option(System.getenv("PORT"))

    val port:Int = envPort match {
      case Some(p) => Integer.valueOf(p)
      case None => basePort
    }

    val service: Service[HttpRequest, HttpResponse] = fileService andThen appService

    val server: Server = ServerBuilder()
      .codec(Http())
      .bindTo(new InetSocketAddress(port))
      .name("finatraServer")
      .build(service)

    val pid = ManagementFactory.getRuntimeMXBean().getName().split('@').head

    val pidFile = new File(pidPath)
    val pidFileStream = new FileOutputStream(pidFile)
    pidFileStream.write(pid.getBytes)
    pidFileStream.close

    logger.info("process %s started on %s, docroot %s", pid, port, docroot)
    println("process " + pid + " started on port: " + port.toString + " view logs/finatra.log for more info")
  }

  Runtime.getRuntime().addShutdownHook(
    new Thread(new Runnable() {
      override def run() { FinatraServer.shutdown }
    }))

}

