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

import com.twitter.finagle.http.{Request => FinagleRequest, Response => FinagleResponse, _}
import com.twitter.finagle._
import java.lang.management.ManagementFactory
import com.twitter.util.Await
import com.twitter.server.TwitterServer
import com.twitter.finagle.Http
import org.jboss.netty.handler.codec.http.{HttpResponse, HttpRequest}
import com.twitter.app.App




object FinatraServer {

  val fs: FinatraServer = new FinatraServer

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

class FinatraServer extends TwitterServer {

  override val name = "finatra"

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

  private[this] val nettyToFinagle =
    Filter.mk[HttpRequest, HttpResponse, FinagleRequest, FinagleResponse] { (req, service) =>
      service(FinagleRequest(req)) map { _.httpResponse }
    }

  def start() {

    val appService  = new AppService(controllers)
    val fileService = new FileService

    addFilter(fileService)

    val service = nettyToFinagle andThen allFilters(appService)

    //val http = Http().maxRequestSize(Config.getInt("max_request_megabytes").megabyte)

    val server = Http.serve(Config.port(), service)

    log.info("process %s started on %s", pid, Config.port())

    println("finatra process " + pid + " started on port: " + Config.port())

    onExit {
      server.close()
    }

    Await.ready(server)

  }
}


