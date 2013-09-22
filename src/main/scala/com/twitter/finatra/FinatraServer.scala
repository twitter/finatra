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

import com.twitter.finagle.http.{Request => FinagleRequest, Response => FinagleResponse}
import com.twitter.finagle.{Filter, Service, SimpleFilter, Http}
import java.lang.management.ManagementFactory
import com.twitter.util.Await
import org.jboss.netty.handler.codec.http.{HttpResponse, HttpRequest}

class FinatraServer extends FinatraTwitterServer {

  val controllers:  ControllerCollection = new ControllerCollection
  var filters:      Seq[SimpleFilter[FinagleRequest, FinagleResponse]] = Seq.empty
  val pid:          String = ManagementFactory.getRuntimeMXBean.getName.split('@').head

  def allFilters(baseService: Service[FinagleRequest, FinagleResponse]):
    Service[FinagleRequest, FinagleResponse] = {
      filters.foldRight(baseService) { (b,a) => b andThen a }
  }

  def register(app: Controller) { controllers.add(app) }

  def addFilter(filter: SimpleFilter[FinagleRequest, FinagleResponse]) {
    filters = filters ++ Seq(filter)
  }

  private[this] val nettyToFinagle =
    Filter.mk[HttpRequest, HttpResponse, FinagleRequest, FinagleResponse] { (req, service) =>
      service(FinagleRequest(req)) map { _.httpResponse }
    }

  def main() {
    start()
  }

  def start() {

    val appService  = new AppService(controllers)
    val fileService = new FileService
    val loggingFilter = new LoggingFilter

    addFilter(loggingFilter)
    addFilter(fileService)

    val service = nettyToFinagle andThen allFilters(appService)

    //val http = Http().maxRequestSize(Config.getInt("max_request_megabytes").megabyte)

    val server = Http.serve(config.port(), service)

    //val server: Server =
    //  ((certPathOpt, keyPathOpt) match {
    //    case (Some(cert), Some(key)) => serverBuilder.tls(cert, key)
    //    case _ => serverBuilder
    //  }).build(service)

    log.info("finatra process " + pid + " started on port: " + config.port())
    log.info("admin server started on port: " + config.adminPort())

    onExit {
      server.close()
    }

    Await.ready(server)

  }
}


