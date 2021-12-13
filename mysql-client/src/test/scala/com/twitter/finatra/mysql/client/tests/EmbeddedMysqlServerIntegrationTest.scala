package com.twitter.finatra.mysql.client.tests

import com.twitter.finagle.mysql.ServerError
import com.twitter.finagle.mysql.harness.config.User
import com.twitter.finatra.mysql.EmbeddedMysqlServer
import com.twitter.inject.Test
import java.nio.file.Paths

class EmbeddedMysqlServerIntegrationTest extends Test with IgnoreMysqlHarnessIfUnavailable {

  test("EmbeddedMysqlServer#can start", MysqlHarnessTag) {
    val server = EmbeddedMysqlServer()
    try {
      server.start()
      server.assertStarted()
      server.close()
    } finally {
      server.close()
    }
  }

  test("EmbeddedMysqlServer#can retrieve a client", MysqlHarnessTag) {
    val server = EmbeddedMysqlServer()
    try {
      val clnt = server.mysqlClient
      server.assertStarted()
      server.close()
      assert(clnt.isClosed)
    } finally {
      server.close()
    }
  }

  test("EmbeddedMysqlServer#retrieving a client always creates a new instance", MysqlHarnessTag) {
    val server = EmbeddedMysqlServer()
    try {
      val clnt = server.mysqlClient
      server.assertStarted()
      val clntB = server.mysqlClient
      val clntC = server.mysqlClient
      assert((clnt eq clntB) == false)
      assert((clnt eq clntC) == false)
      server.close()
      assert(clnt.isClosed)
      assert(clntB.isClosed)
      assert(clntC.isClosed)
    } finally {
      server.close()
    }
  }

  test(
    "EmbeddedMysqlServer#retrieving client for invalid user credentials is allowed",
    MysqlHarnessTag) {
    val user1 = User("user1", Some("pass1"), User.Permission.Select)
    val user2 = User("user2", Some("pass2"), User.Permission.Select)
    val user3 = User("user3", Some("pass3"), User.Permission.Select)
    val server = EmbeddedMysqlServer.newBuilder
      .withUsers(Seq(user1, user2, user3))
      .newServer()

    try {
      server.start()
      server.assertStarted()
      val clnt = server.mysqlClient(User("user1", Some("notgoodpass"), User.Permission.Select))

      // but the query for that user will fail (lazy handshake error)
      intercept[ServerError] {
        await(clnt.query("SELECT 1"))
      }
    } finally {
      server.close()
    }
  }

  test("EmbeddedMysqlServer#fails to start with bad config") {
    val server =
      EmbeddedMysqlServer.newBuilder.withPath(Paths.get("/tmp/mysql/not_extracted")).newServer()
    try {
      intercept[Exception] {
        server.start()
      }
      server.assertStarted(false)
    } finally {
      server.close()
    }
  }

  test("EmbeddedMysqlServer#cannot start a closed server", MysqlHarnessTag) {
    val server = EmbeddedMysqlServer()
    try {
      server.start()
      server.assertStarted()
      server.close()
      intercept[IllegalStateException] {
        server.start()
      }
    } finally {
      server.close()
    }
  }

}
