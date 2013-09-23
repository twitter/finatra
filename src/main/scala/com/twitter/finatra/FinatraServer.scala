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
import com.twitter.finagle._
import java.lang.management.ManagementFactory
import com.twitter.util.Await
import org.jboss.netty.handler.codec.http.{HttpResponse, HttpRequest}
import com.twitter.finagle.netty3.{Netty3ListenerTLSConfig, Netty3Listener}
import java.net.SocketAddress
import com.twitter.conversions.storage._
import com.twitter.finagle.server.DefaultServer
import com.twitter.finagle.dispatch.SerialServerDispatcher
import com.twitter.finagle.ssl.Ssl

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
    var secureServer: Option[ListeningServer] = None

    val codec = http.Http()
              .maxRequestSize(config.maxRequestSize().megabyte)
              .enableTracing(true)
              .server(ServerCodecConfig("httpserver", new SocketAddress{}))
              .pipelineFactory

      //Setup HTTPS Server
      if (!config.certificatePath().isEmpty && !config.keyPath().isEmpty) {
        val tlsConfig =
          Some(Netty3ListenerTLSConfig(() => Ssl.server(config.certificatePath(), config.keyPath(), null, null, null)))
        object HttpsListener extends Netty3Listener[HttpResponse, HttpRequest]("https", codec, tlsConfig = tlsConfig)
        object HttpsServer extends DefaultServer[HttpRequest, HttpResponse, HttpResponse, HttpRequest](
          "https", HttpsListener, new SerialServerDispatcher(_, _)
        )
        log.info("https server started on port: " + config.sslPort())
        secureServer = Some(HttpsServer.serve(config.sslPort(), service))
      } else {
        None
      }

    object HttpListener extends Netty3Listener[HttpResponse, HttpRequest]("http", codec)

    object HttpServer extends DefaultServer[HttpRequest, HttpResponse, HttpResponse, HttpRequest](
      "http", HttpListener, new SerialServerDispatcher(_, _)
    )

    val server = HttpServer.serve(config.port(), service)

    log.info("finatra process " + pid + " started")
    log.info("http server started on port: " + config.port())
    log.info("admin server started on port: " + config.adminPort())

    onExit {
      secureServer map { _.close() }
      server.close()
    }

    Await.ready(server)

    secureServer map { Await.ready(_) }

  }
}


