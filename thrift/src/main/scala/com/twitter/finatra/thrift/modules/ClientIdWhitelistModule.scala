package com.twitter.finatra.thrift.modules

import com.google.inject.Provides
import com.twitter.finagle.thrift.ClientId
import com.twitter.inject.{Logging, TwitterModule}
import javax.inject.Singleton
import org.yaml.snakeyaml.Yaml

object ClientIdWhitelistModule
  extends TwitterModule
  with Logging {

  @Provides
  @Singleton
  def providesWhitelist: Set[ClientId] = {
    val yamlList = new Yaml().
      load(ClientIdWhitelistModule.getClass.getResourceAsStream("/client_whitelist.yml")).
      asInstanceOf[java.util.ArrayList[String]]
    val clientIds = Set(yamlList.toArray: _*) map { _.toString } map ClientId.apply
    info(s"Client id whitelist loaded ${clientIds.size} ids")
    clientIds
  }
}
