/**
 * Copyright (C) 2012 Twitter Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.twitter.finatra

import com.twitter.finagle.builder.{Server, ServerBuilder}
import com.twitter.finagle.http.{Request => FinagleRequest, Response => FinagleResponse, _}
import com.twitter.finagle._
import com.twitter.logging.config._
import com.twitter.logging.{FileHandler, LoggerFactory, Logger}
import java.lang.management.ManagementFactory
import java.net.InetSocketAddress
import com.twitter.finagle.tracing.{Tracer, NullTracer}
import com.twitter.conversions.storage._
import com.twitter.util.StorageUnit
import com.twitter.server.TwitterServer
import com.twitter.finagle.Http
import org.jboss.netty.handler.codec.http.{HttpResponse, HttpRequest}
import com.twitter.logging.LoggerFactory

object FinatraServer {

  var fs: FinatraServer = new FinatraServer

  def register(app: Controller) {
    fs.register(app)
  }

  def start() {
    fs.start()
  }

  def addFilter(filter: SimpleFilter[FinagleRequest, FinagleResponse]) {
    fs.addFilter(filter)
  }

}

class FinatraServer extends TwitterServer with Logging {

  @volatile private[this] var server: Option[Server] = None

  val controllers:  ControllerCollection = new ControllerCollection
  var filters:      Seq[SimpleFilter[FinagleRequest, FinagleResponse]] =
    Seq.empty
  val pid:          String =
    ManagementFactory.getRuntimeMXBean().getName().split('@').head

  def allFilters(baseService: Service[FinagleRequest, FinagleResponse]):
    Service[FinagleRequest, FinagleResponse] = {
      filters.foldRight(baseService) { (b,a) => b andThen a }
  }

  def register(app: Controller) { controllers.add(app) }

  def addFilter(filter: SimpleFilter[FinagleRequest, FinagleResponse]) {
    filters = filters ++ Seq(filter)
  }

  def initLogger() {

    val handler = FileHandler(
        filename = "log/finatra.log",
        rollPolicy = Policy.Never,
        append = false,
        level = Some(Level.INFO))

    val log: Logger = LoggerFactory(
      node = "com.twitter",
      level = Some(Level.DEBUG),
      handlers = List(handler)).apply()

  }

//  def initAdminService(runtimeEnv: RuntimeEnvironment) {
//      AdminServiceFactory(
//        httpPort = Config.getInt("stats_port"),
//        statsNodes = StatsFactory(
//          reporters = JsonStatsLoggerFactory(serviceName = Some("finatra")) ::
//                  TimeSeriesCollectorFactory() :: Nil
//        ) :: Nil
//      )(runtimeEnv)
//  }


  def shutdown() {
    logger.info("shutting down")
    println("finatra process shutting down")
    server foreach { s => s.close()() }
    System.exit(0)
  }

  def start() {



//    if(Config.getBool("stats_enabled")){
//      initAdminService(runtimeEnv)
//    }

    val nettyToFinagle =
      Filter.mk[HttpRequest, HttpResponse, FinagleRequest, FinagleResponse] { (req, service) =>
        service(FinagleRequest(req)) map { _.httpResponse }
      }

    initLogger()

    val appService  = new AppService(controllers)
    val fileService = new FileService

    addFilter(fileService)

    val port = Config.getInt("port")
    val service = nettyToFinagle andThen allFilters(appService)


//    val http = Http().maxRequestSize(Config.getInt("max_request_megabytes").megabyte)


    val server = Http.serve(":8888", service)

    logger.info("process %s started on %s", pid, port)

    println("finatra process " + pid + " started on port: " + port.toString)
    println("config args:")
    Config.printConfig()

  }
}


