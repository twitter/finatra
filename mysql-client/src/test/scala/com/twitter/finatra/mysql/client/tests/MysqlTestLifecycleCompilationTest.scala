package com.twitter.finatra.mysql.client.tests

import com.twitter.finatra.mysql.EmbeddedMysqlServer
import com.twitter.finatra.mysql.MysqlTestLifecycle
import com.twitter.inject.Test

class MysqlTestLifecycleCompilationTest
    extends Test
    with MysqlTestLifecycle
    with IgnoreMysqlHarnessIfUnavailable {

  protected final val embeddedMysqlServer = EmbeddedMysqlServer()

  lazy val mysqlClient = embeddedMysqlServer.mysqlClient

  test("TestLifecycle#start())", MysqlHarnessTag) {
    embeddedMysqlServer.start()
    embeddedMysqlServer.assertStarted()
  }

  test("TestLifecycle#new client", MysqlHarnessTag) {
    await(mysqlClient.query("SELECT 1"))
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    assert(embeddedMysqlServer.isClosed())
  }

}
