package com.twitter.finatra.thrift.modules

import com.google.inject.Provides
import com.twitter.finagle.thrift.ClientId
import com.twitter.inject.{Logging, TwitterModule}
import javax.inject.Singleton
import org.yaml.snakeyaml.Yaml

@deprecated("Use ClientIdAcceptlistModule", "2018-11-08")
object ClientIdWhitelistModule extends TwitterModule with Logging {

  @Provides
  @Singleton
  final def provideslist: Set[ClientId] =
    ClientIdAcceptlistModule.loadAcceptlist("/client_whitelist.yml")
}

private[modules] object ClientIdAcceptlistModule extends Logging {

  def loadAcceptlist(filename: String): Set[ClientId] = {
    val yamlList = new Yaml()
      .load(ClientIdAcceptlistModule.getClass.getResourceAsStream(filename))
      .asInstanceOf[java.util.ArrayList[String]]
    val clientIds = Set(yamlList.toArray: _*) map { _.toString } map ClientId.apply
    info(s"Client id accept list loaded ${clientIds.size} client ids.")
    clientIds
  }
}

class ClientIdAcceptlistModule(filename: String)
  extends TwitterModule {
  import ClientIdAcceptlistModule._

  @Provides
  @Singleton
  final def providesAcceptlist: Set[ClientId] = loadAcceptlist(filename)
}
