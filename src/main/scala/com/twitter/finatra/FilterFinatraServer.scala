package com.twitter.finatra

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

import com.twitter.finagle.builder.{Server, ServerBuilder}


import com.twitter.finagle.http._
import com.twitter.finagle.http.{Request => FinagleRequest, Response => FinagleResponse}
import com.twitter.logging.config._
import com.twitter.logging.Logger
import java.lang.management.ManagementFactory
import java.net.InetSocketAddress
import com.twitter.finagle.tracing.{Tracer, NullTracer}
import com.twitter.finagle.{Filter, Service, SimpleFilter}
import com.twitter.util.Future


object FilterFinatraServer {

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

class FilterFinatraServer(val controller: FilteredController) extends Logging {

  val pid = ManagementFactory.getRuntimeMXBean().getName().split('@').head

  def initLogger() {
    val config = new LoggerConfig {
      node = Config.get("log_node")
      level = Logger.INFO
      handlers = new FileHandlerConfig {
        filename = Config.get("log_path")
        roll = Policy.SigHup
      }
    }
    config()
  }

  def start(tracerFactory: Tracer.Factory = NullTracer.factory) {

    initLogger()

    //    val appService = new AppService(controllers)
    //    val fileService = new FileService
    //    val errorService = new ErrorFilter
    //
    //    addFilter(errorService)
    //    addFilter(fileService)
    //
    val port = Config.getInt("port")

    val service: Service[FinagleRequest, FinagleResponse] = FinagleFinatraFilter.andThen(controller.asFilter).andThen(NotFoundService)

    val server: Server = ServerBuilder()
      .codec(new RichHttp[FinagleRequest](Http()))
      .bindTo(new InetSocketAddress(port))
      .tracerFactory(tracerFactory)
      .name(Config.get("name"))
      .build(service)

    logger.info("process %s started on %s", pid, port)

    println("finatra process " + pid + " started on port: " + port.toString)
    println("config args:")
    Config.printConfig()

  }

}

object FinagleFinatraFilter extends Filter[FinagleRequest, FinagleResponse, Request, Response] {
  def apply(rawRequest: FinagleRequest, service: Service[Request, Response]) = service.apply(RequestAdapter(rawRequest)).map(_.build)
}

object NotFoundException extends Exception

object NotFoundService extends Service[Request, Response] {
  def apply(request: Request) = Future.exception(NotFoundException)
}



