package com.twitter.finatra.mysql.client.tests

import com.twitter.finagle.mysql.ServerError
import com.twitter.finagle.mysql.harness.config.User
import com.twitter.finatra.mysql.EmbeddedMysqlServer
import com.twitter.finatra.mysql.IgnoreMysqlHarnessIfUnavailable
import com.twitter.finatra.mysql.MysqlHarnessTag
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
    val user1 = User("user1", Some("pass1"), User.Permission.Select)
    val server = EmbeddedMysqlServer.newBuilder
      .withDatabaseName("databaseWithUser")
      .withUsers(Seq(user1))
      .newServer()
    val clnt = server.mysqlClient(user1)
    val clntB = server.mysqlClient(user1)
    val clntC = server.mysqlClient(user1)
    try {
      server.assertStarted()
      assert((clnt eq clntB) == false)
      assert((clnt eq clntC) == false)
    } finally {
      server.close()
      assert(clnt.isClosed)
      assert(clntB.isClosed)
      assert(clntC.isClosed)
    }
  }

  test("EmbeddedMysqlServer#only creates a single root client", MysqlHarnessTag) {
    val server = EmbeddedMysqlServer()
    val clnt = server.mysqlClient
    val clntB = server.mysqlClient
    try {
      server.assertStarted()
      assert((clnt eq clntB) == true)
    } finally {
      server.close()
      assert(clnt.isClosed)
      assert(clntB.isClosed)
    }
  }

  test(
    "EmbeddedMysqlServer#retrieving client for invalid user credentials is allowed",
    MysqlHarnessTag) {
    val user1 = User("user1", Some("pass1"), User.Permission.Select)
    val user2 = User("user2", Some("pass2"), User.Permission.Select)
    val user3 = User("user3", Some("pass3"), User.Permission.Select)
    val server = EmbeddedMysqlServer.newBuilder
      .withDatabaseName("invalidCredentialsDb")
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
