package com.twitter.finatra.http

import com.twitter.conversions.DurationOps._
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.inject.server.{EmbeddedTwitterServer, PortUtils, Ports, info}
import com.twitter.util.Closable
import net.codingwell.scalaguice.typeLiteral
import scala.collection.JavaConverters._

/** Internal utility which represents an http client to external interfaces of an [[EmbeddedTwitterServer]] */
private[twitter] trait ExternalHttpClient { self: EmbeddedTwitterServer =>

  /**
   * Underlying Embedded TwitterServer exposed as a [[com.twitter.inject.server.Ports]]
   * @return the underlying TwitterServer as a [[com.twitter.inject.server.Ports]].
   */
  def twitterServer: Ports

  /**
   * The expected flag that sets the external port for serving the underlying Thrift service.
   * @return a String representing the Http port flag.
   * @see [[com.twitter.app.Flag]]
   */
  def httpPortFlag: String = "http.port"

  /** Provide an override to the underlying server's mapper */
  def mapperOverride: Option[FinatraObjectMapper]

  /** Provide an override to the external HTTPS client */
  private[twitter] def httpsClientOverride: Option[JsonAwareEmbeddedHttpClient] = None

  /* Overrides */

  /** Logs the external http and/or https host and port of the underlying EmbeddedHttpServer */
  override protected[twitter] def logStartup(): Unit = {
    self.logStartup()
    if (twitterServer.httpExternalPort.isDefined) {
      info(s"ExternalHttp   -> http://$externalHttpHostAndPort", disableLogging)
    }
    if (twitterServer.httpsExternalPort.isDefined) {
      info(s"ExternalHttps  -> https://$externalHttpsHostAndPort", disableLogging)
    }
  }

  /**
   * Adds the [[httpPortFlag]] with a value pointing to the ephemeral loopback address to
   * the list of flags to be passed to the underlying server.
   * @see [[PortUtils.ephemeralLoopback]].
   */
  override protected[twitter] def combineArgs(): Array[String] = {
    s"-$httpPortFlag=${PortUtils.ephemeralLoopback}" +: self.combineArgs
  }

  /* Public */

  /** A `host:post` String of the loopback and external "http" port for the underlying embedded HttpServer */
  lazy val externalHttpHostAndPort: String = {
    PortUtils.loopbackAddressForPort(httpExternalPort())
  }

  /** A `host:post` String of the loopback and external "https" port for the underlying embedded HttpServer */
  lazy val externalHttpsHostAndPort: String = {
    PortUtils.loopbackAddressForPort(httpsExternalPort())
  }

  /** Supplements an absolute path URI with the http scheme and authority */
  def fullHttpURI(path: String): String = {
    s"http://$externalHttpHostAndPort$path"
  }

  /** The assigned external "http" port for the underlying embedded HttpServer */
  def httpExternalPort(): Int = {
    start()
    twitterServer.httpExternalPort.get
  }

  /** The assigned external "https" port for the underlying embedded HttpServer */
  def httpsExternalPort(): Int = {
    start()
    twitterServer.httpsExternalPort.get
  }

  /**
   * The embedded [[FinatraObjectMapper]]. When the underlying embedded HttpServer is an injectable
   * TwitterServer and has configured an object mapper, this will represent the server's configured
   * object mapper, otherwise it is a default instantiation of the [[FinatraObjectMapper]].
   *
   * @see [[FinatraObjectMapper.create(injector: Injector)]]
   */
  final lazy val mapper: FinatraObjectMapper = {
    if (isInjectable) {
      // if there is an object mapper bound, use it as the default otherwise create a new one
      val default =
        if (injector.underlying.findBindingsByType(typeLiteral[FinatraObjectMapper]).asScala.nonEmpty)
          injector.instance[FinatraObjectMapper]
        else FinatraObjectMapper.create()
      mapperOverride.getOrElse(default)
    } else {
      FinatraObjectMapper.create()
    }
  }

  final lazy val httpClient: JsonAwareEmbeddedHttpClient = {
    val client = new JsonAwareEmbeddedHttpClient(
      "httpClient",
      httpExternalPort(),
      tls = false,
      sessionAcquisitionTimeout = 1.second,
      streamResponses = streamResponse,
      defaultHeaders = () => defaultRequestHeaders,
      mapper,
      disableLogging = self.disableLogging
    )
    closeOnExit {
      if (isStarted) {
        Closable.make { deadline =>
          info(s"Closing embedded http client: ${client.label}", disableLogging)
          client.close(deadline)
        }
      } else Closable.nop
    }
    client
  }

  final lazy val httpsClient: JsonAwareEmbeddedHttpClient = httpsClientOverride.getOrElse {
    val client = new JsonAwareEmbeddedHttpClient(
      "httpsClient",
      httpsExternalPort(),
      tls = true,
      sessionAcquisitionTimeout = 1.second,
      streamResponses = streamResponse,
      defaultHeaders = () => defaultRequestHeaders,
      mapper,
      disableLogging = self.disableLogging
    )
    closeOnExit {
      if (isStarted) {
        Closable.make { deadline =>
          info(s"Closing embedded http client: ${client.label}", disableLogging)
          client.close(deadline)
        }
      } else Closable.nop
    }
    client
  }
}
