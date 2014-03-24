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

import com.twitter.finagle.http.{Request => FinagleRequest, Response => FinagleResponse, HttpMuxer}
import com.twitter.finagle._
import java.lang.management.ManagementFactory
import com.twitter.util.{Future, Await}
import org.jboss.netty.handler.codec.http.{HttpResponse, HttpRequest}
import com.twitter.finagle.netty3.{Netty3ListenerTLSConfig, Netty3Listener}
import java.net.SocketAddress
import com.twitter.conversions.storage._
import com.twitter.finagle.server.DefaultServer
import com.twitter.finagle.dispatch.SerialServerDispatcher
import com.twitter.finagle.ssl.Ssl
import java.io.{FileOutputStream, File, FileNotFoundException}

class FinatraServer extends FinatraTwitterServer {

  val controllers:  ControllerCollection = new ControllerCollection
  var filters:      Seq[Filter[FinagleRequest, FinagleResponse,FinagleRequest, FinagleResponse]] = Seq.empty
  val pid:          String = ManagementFactory.getRuntimeMXBean.getName.split('@').head

  var secureServer: Option[ListeningServer] = None
  var server:       Option[ListeningServer] = None
  var adminServer:  Option[ListeningServer] = None

  def allFilters(baseService: Service[FinagleRequest, FinagleResponse]):
    Service[FinagleRequest, FinagleResponse] = {
      filters.foldRight(baseService) { (b,a) => b andThen a }
  }

  def register(app: Controller) { controllers.add(app) }

  def addFilter(filter: Filter[FinagleRequest, FinagleResponse,FinagleRequest, FinagleResponse]) {
    filters = filters ++ Seq(filter)
  }

  def main() {
    log.info("finatra process " + pid + " started")
    start()
  }

  private[this] val nettyToFinagle =
    Filter.mk[HttpRequest, HttpResponse, FinagleRequest, FinagleResponse] { (req, service) =>
      service(FinagleRequest(req)) map { _.httpResponse }
    }

  private[this] lazy val service = {
    val appService  = new AppService(controllers)
    val fileService = new FileService
    val loggingFilter = new LoggingFilter

    addFilter(loggingFilter)
    addFilter(fileService)

    nettyToFinagle andThen allFilters(appService)
  }

  private[this] lazy val codec = {
    http.Http()
      .maxRequestSize(config.maxRequestSize().megabyte)
      .enableTracing(true)
      .server(ServerCodecConfig("httpserver", new SocketAddress{}))
      .pipelineFactory
  }

  def writePidFile() {
    val pidFile = new File(config.pidPath())
    val pidFileStream = new FileOutputStream(pidFile)
    pidFileStream.write(pid.getBytes)
    pidFileStream.close()
  }

  def removePidFile() {
    val pidFile = new File(config.pidPath())
    pidFile.delete()
  }

  def startSecureServer() {
    val tlsConfig =
      Some(Netty3ListenerTLSConfig(() => Ssl.server(config.certificatePath(), config.keyPath(), null, null, null)))
    object HttpsListener extends Netty3Listener[HttpResponse, HttpRequest]("https", codec, tlsConfig = tlsConfig)
    object HttpsServer extends DefaultServer[HttpRequest, HttpResponse, HttpResponse, HttpRequest](
      "https", HttpsListener, new SerialServerDispatcher(_, _)
    )
    log.info("https server started on port: " + config.sslPort())
    secureServer = Some(HttpsServer.serve(config.sslPort(), service))
  }

  def startHttpServer() {
    object HttpListener extends Netty3Listener[HttpResponse, HttpRequest]("http", codec)
    object HttpServer extends DefaultServer[HttpRequest, HttpResponse, HttpResponse, HttpRequest](
      "http", HttpListener, new SerialServerDispatcher(_, _)
    )
    log.info("http server started on port: " + config.port())
    server = Some(HttpServer.serve(config.port(), service))
  }

  def startAdminServer() {
    log.info("admin http server started on port: " + config.adminPort())
    adminServer = Some(HttpServer.serve(config.adminPort(), HttpMuxer))
  }

  def stop() {
    server map { _.close() }
    secureServer map { _.close() }
    adminServer map { _.close() }
  }

  onExit {
    stop()
    removePidFile()
  }

  def start() {

    if (!config.pidPath().isEmpty) {
      writePidFile()
    }

    if (!config.port().isEmpty) {
      startHttpServer()
    }

    if (!config.adminPort().isEmpty) {
      startAdminServer()
    }

    if (!config.certificatePath().isEmpty && !config.keyPath().isEmpty) {
      if (!new File(config.certificatePath()).canRead){
        val e = new FileNotFoundException("SSL Certificate not found: " + config.certificatePath())
        log.fatal(e, "SSL Certificate could not be read: " + config.certificatePath())
        throw e
      }
      if (!new File(config.keyPath()).canRead){
        val e = new FileNotFoundException("SSL Key not found: " + config.keyPath())
        log.fatal(e, "SSL Key could not be read: " + config.keyPath())
        throw e
      }
      startSecureServer()
    }

    server       map { Await.ready(_) }
    adminServer  map { Await.ready(_) }
    secureServer map { Await.ready(_) }

  }
}


