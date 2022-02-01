package com.twitter.finatra.mysql.client.tests

import com.google.inject.Module
import com.google.inject.Provides
import com.google.inject.Singleton
import com.twitter.finagle.mysql.Result
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.mysql.EmbeddedMysqlServer
import com.twitter.finatra.mysql.client.Config
import com.twitter.finatra.mysql.client.Credentials
import com.twitter.finatra.mysql.client.MysqlClient
import com.twitter.finatra.mysql.client.modules.MysqlClientModuleTrait
import com.twitter.inject.Injector
import com.twitter.inject.Test
import com.twitter.inject.server.EmbeddedTwitterServer
import com.twitter.inject.server.TwitterServer
import com.twitter.util.Future
import com.twitter.util.mock.Mockito

class MysqlClientModuleTraitTest extends Test with Mockito with IgnoreMysqlHarnessIfUnavailable {

  class MyTestMysqlClientModule extends MysqlClientModuleTrait {
    override def label: String = "testMysql"
    override def dest: String = "/s/test/dest"

    @Provides
    @Singleton
    protected def provideCredentials(): Credentials = Credentials("user", "password")

    @Provides
    @Singleton
    protected def provideConfig(credentials: Credentials): Config =
      Config("database_a", credentials)

    @Provides
    @Singleton
    protected def provideMysqlClient(
      injector: Injector,
      statsReceiver: StatsReceiver,
      config: Config
    ): MysqlClient =
      newMysqlClient(injector, statsReceiver, config)
  }

  test("MysqlClientModuleTrait#can be injected") {
    var pingIssued = false
    val server = new EmbeddedTwitterServer(
      new TwitterServer {
        override def modules: Seq[Module] = Seq(new MyTestMysqlClientModule)
        protected override def postInjectorStartup(): Unit = {
          await(injector.instance[MysqlClient].ping())
          pingIssued = true
          super.postInjectorStartup()
        }
      }
    )
    try {
      assert(pingIssued == false)
      server.start()
      server.assertHealthy(true)
      assert(pingIssued == true)
    } finally {
      server.close()
    }
  }

  test("MysqlClientModuleTrait#can bind over credentials") {
    val credentials = Credentials("userReplaced", "passReplaced")
    val server = new EmbeddedTwitterServer(
      new TwitterServer {
        override def modules: Seq[Module] = Seq(new MyTestMysqlClientModule)
      }
    ).bind[Credentials].toInstance(credentials)

    try {
      server.start()
      server.assertHealthy(true)
      server.injector.instance[Credentials] should be(credentials)
    } finally {
      server.close()
    }
  }

  test("MysqlClientModuleTrait#can bind over config") {
    val config = Config("database_xyz", Credentials("user1", "pass1"))
    val server = new EmbeddedTwitterServer(
      new TwitterServer {
        override def modules: Seq[Module] = Seq(new MyTestMysqlClientModule)
      }
    ).bind[Config].toInstance(config)

    try {
      server.start()
      server.assertHealthy(true)
      server.injector.instance[Config] should be(config)
    } finally {
      server.close()
    }
  }

  test("MysqlClientModuleTrait#can bind over MysqlClient", MysqlHarnessTag) {
    val embeddedMysql = EmbeddedMysqlServer.newBuilder().withLazyStart.newServer()
    val mysqlClient = embeddedMysql.mysqlClient
    val server = new EmbeddedTwitterServer(
      new TwitterServer {
        override def modules: Seq[Module] = Seq(new MyTestMysqlClientModule)
      }
    ).bind[MysqlClient].toInstance(mysqlClient)

    try {
      server.start()
      server.assertHealthy(true)
      server.injector.instance[MysqlClient] should be(mysqlClient)
    } finally {
      server.close()
      embeddedMysql.close()
    }
  }

  test("MysqlClientModuleTrait#can bind mock MysqlClient") {
    val mysqlClient = mock[MysqlClient]
    mysqlClient.ping() returns Future.Done
    mysqlClient.query("SELECT * FROM ABC") returns Future.value(mock[Result])
    val server = new EmbeddedTwitterServer(
      new TwitterServer {
        override def modules: Seq[Module] = Seq(new MyTestMysqlClientModule)

        override def postInjectorStartup(): Unit = {
          await(injector.instance[MysqlClient].ping())
          await(injector.instance[MysqlClient].query("SELECT * FROM ABC"))
          super.postInjectorStartup()
        }
      }
    ).bind[MysqlClient].toInstance(mysqlClient)

    try {
      server.start()
      server.assertHealthy(true)
    } finally {
      server.close()
    }
  }

}
