package com.twitter.finatra.http

import com.twitter.finagle.Http
import com.twitter.finagle.ssl.KeyCredentials
import com.twitter.finagle.ssl.server.SslServerConfiguration
import java.io.File

/**
 * A helper trait for serving HTTPS requests with standard TLS. To use,
 * mix into an [[HttpServer]] and pass the `cert.path` and `key.path` flags
 * and optionally `chain.path` flag if using a custom chain or set the defaults accordingly.
 *
 * {{{
 *   object MyServiceMain extends MyService
 *
 *   class MyService extends HttpServer with Tls {
 *      override val defaultCertificatePath = "/path/to/cert"
 *
 *      override protected def configureHttp(router: HttpRouter): Unit = {
 *         ...
 *      }
 *   }
 * }}}
 *
 * To further specify or change the [[Http.Server]] configuration, override the [[Tls#configureHttpsServer]]
 * method.
 *
 * @see [[https://en.wikipedia.org/wiki/Transport_Layer_Security TLS]]
 */
trait Tls { self: HttpServer =>

  protected def defaultCertificatePath: String = ""

  private val certificatePathFlag =
    flag("cert.path", defaultCertificatePath, "path to SSL certificate")

  protected def defaultKeyPath: String = ""

  private val keyPathFlag =
    flag("key.path", defaultKeyPath, "path to SSL key")

  protected def defaultChainPath: String = ""

  private val chainPathFlag =
    flag("chain.path", defaultChainPath, "path to SSL chain")

  override protected def configureHttpsServer(server: Http.Server): Http.Server = {
    val chainPath = chainPathFlag()
    val keyCredentials =
      if (chainPath.isEmpty) {
        KeyCredentials.CertAndKey(
          certificateFile = new File(certificatePathFlag()),
          keyFile = new File(keyPathFlag()))
      } else {
        KeyCredentials.CertKeyAndChain(
          certificateFile = new File(certificatePathFlag()),
          keyFile = new File(keyPathFlag()),
          caCertificateFile = new File(chainPath))
      }

    server.withTransport
      .tls(SslServerConfiguration(keyCredentials))
  }
}
