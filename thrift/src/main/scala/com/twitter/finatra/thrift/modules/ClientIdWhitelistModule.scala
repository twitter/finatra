package com.twitter.finatra.thrift.modules

import com.google.inject.Provides
import com.twitter.config.yaml.YamlList
import com.twitter.finagle.thrift.ClientId
import com.twitter.inject.{Logging, TwitterModule}
import javax.inject.Singleton

object ClientIdWhitelistModule
  extends TwitterModule
  with Logging {

  @Provides
  @Singleton
  def providesWhitelist: Set[ClientId] = {
    val yamlSet = YamlList.load("/client_whitelist.yml").toSet
    val clientIds = yamlSet map { _.toString } map ClientId.apply
    info(s"Client id whitelist loaded ${clientIds.size} ids")
    clientIds
  }
}
