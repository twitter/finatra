package com.twitter.finatra.thrift.tests.doeverything

import java.security.KeyStore
import javax.net.ssl.{SSLEngine, SSLContext, TrustManagerFactory, KeyManagerFactory}

import com.twitter.finagle.ssl.Engine

object TLSConfigurator {
  def serverTLSEngine: Engine = {
    val eng = createSSLEngine("/tls/server.ks", "/tls/server.ts")
    eng.setUseClientMode(false)
    eng.setNeedClientAuth(true)
    Engine(eng)
  }

  def clientTLSEngine: Engine = {
    val eng = createSSLEngine("/tls/client.ks", "/tls/client.ts")
    eng.setUseClientMode(true)
    Engine(eng)
  }

  private def createSSLEngine(keyStorePath: String, trustStorePath: String): SSLEngine = {
    val ks = KeyStore.getInstance("JKS")
    ks.load(getClass.getResourceAsStream(keyStorePath), "".toCharArray)
    val ts = KeyStore.getInstance("JKS")
    ts.load(getClass.getResourceAsStream(trustStorePath), "".toCharArray)
    val kmf = KeyManagerFactory.getInstance("SunX509")
    kmf.init(ks, "".toCharArray)
    val tmf = TrustManagerFactory.getInstance("SunX509")
    tmf.init(ts)

    val context = SSLContext.getInstance("TLS")
    context.init(kmf.getKeyManagers, tmf.getTrustManagers, null)

    context.createSSLEngine()
  }
}
