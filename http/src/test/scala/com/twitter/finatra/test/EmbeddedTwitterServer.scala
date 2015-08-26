package com.twitter.finatra.test

import com.google.inject.Stage
import com.twitter.finatra.http.test.EmbeddedHttpServer
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.inject.server.Ports

@deprecated("Use com.twitter.finatra.http.test.EmbeddedHttpServer or com.twitter.inject.server.EmbeddedTwitterServer", "")
object EmbeddedTwitterServer {

  @deprecated("Use com.twitter.finatra.http.test.EmbeddedHttpServer or com.twitter.inject.server.EmbeddedTwitterServer", "")
  def apply(
    twitterServer: Ports,
    clientFlags: Map[String, String] = Map(),
    resolverMap: Map[String, String] = Map(),
    extraArgs: Seq[String] = Seq(),
    waitForWarmup: Boolean = true,
    stage: Stage = Stage.DEVELOPMENT,
    useSocksProxy: Boolean = false,
    skipAppMain: Boolean = false,
    defaultRequestHeaders: Map[String, String] = Map(),
    defaultHttpSecure: Boolean = false,
    mapper: FinatraObjectMapper = FinatraObjectMapper.create()): EmbeddedHttpServer = {
    new EmbeddedHttpServer(
      twitterServer,
      clientFlags,
      extraArgs,
      waitForWarmup = waitForWarmup,
      stage = stage,
      useSocksProxy = useSocksProxy,
      skipAppMain = skipAppMain,
      defaultRequestHeaders = defaultRequestHeaders,
      defaultHttpSecure = defaultHttpSecure)
  }

  def resolverMapStr(resolverMap: Map[String, String]): Seq[String] = {
    if (resolverMap.isEmpty)
      Seq()
    else
      Seq(
        "-com.twitter.server.resolverMap=" + {
          resolverMap map { case (k, v) =>
            k + "=" + v
          } mkString ","
        })
  }
}

@deprecated("Use com.twitter.finatra.http.test.EmbeddedHttpServer or com.twitter.inject.server.EmbeddedTwitterServer", "")
class EmbeddedTwitterServer(
  twitterServer: Ports,
  clientFlags: Map[String, String] = Map(),
  resolverMap: Map[String, String] = Map(),
  extraArgs: Seq[String] = Seq(),
  waitForWarmup: Boolean = true,
  stage: Stage = Stage.DEVELOPMENT,
  useSocksProxy: Boolean = false,
  skipAppMain: Boolean = false,
  defaultRequestHeaders: Map[String, String] = Map(),
  defaultHttpSecure: Boolean = false,
  mapper: FinatraObjectMapper = FinatraObjectMapper.create())
  extends EmbeddedHttpServer(
    twitterServer,
    clientFlags,
    extraArgs ++ EmbeddedTwitterServer.resolverMapStr(resolverMap),
    waitForWarmup,
    stage,
    useSocksProxy,
    skipAppMain,
    defaultRequestHeaders,
    defaultHttpSecure)