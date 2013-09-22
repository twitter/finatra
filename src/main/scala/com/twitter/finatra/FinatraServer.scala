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
import com.twitter.finagle.http._
import com.twitter.finagle.http.{Request => FinagleRequest, Response => FinagleResponse}
import com.twitter.finagle.{Filter, Service, SimpleFilter, Http}
import java.lang.management.ManagementFactory
import com.twitter.util.Await
import com.twitter.server.TwitterServer
import org.jboss.netty.handler.codec.http.{HttpResponse, HttpRequest}
import com.twitter.app.{GlobalFlag, Flags, App}

object port           extends GlobalFlag[String](":7070", "Http Port")
object adminPort      extends GlobalFlag[String](":9990", "Admin Port")
object env            extends GlobalFlag[String]("development", "Environment")
object appName        extends GlobalFlag[String]("finatra", "Name of server")
object pidEnabled     extends GlobalFlag[Boolean](false, "whether to write pid file")
object pidPath        extends GlobalFlag[String]("finatra.pid", "path to pid file")
object logPath        extends GlobalFlag[String]("logs/finatra.log", "path to log")
object templatePath   extends GlobalFlag[String]("/", "path to templates")
object docroot        extends GlobalFlag[String]("src/main/resources", "path to docroot")
object maxRequestSize extends GlobalFlag[Int](5, "size of max request")

class FinatraServer extends TwitterServer {

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

    addFilter(fileService)

    val service = nettyToFinagle andThen allFilters(appService)

    //val http = Http().maxRequestSize(Config.getInt("max_request_megabytes").megabyte)

    val server = Http.serve(port(), service)

    log.info("process %s started on %s", pid, port())
    //val server: Server =
    //  ((certPathOpt, keyPathOpt) match {
    //    case (Some(cert), Some(key)) => serverBuilder.tls(cert, key)
    //    case _ => serverBuilder
    //  }).build(service)

    log.info("finatra process " + pid + " started on port: " + port())

    onExit {
      server.close()
    }

    Await.ready(server)

  }
}


