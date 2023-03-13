package com.twitter.finatra.mysql.util

import com.twitter.finagle.mysql._
import com.twitter.finagle.mysql.harness.config.User
import com.twitter.finatra.mysql.EmbeddedMysqlServer
import com.twitter.finatra.mysql.IgnoreMysqlHarnessIfUnavailable
import com.twitter.finatra.mysql.MysqlHarnessTag
import com.twitter.finatra.mysql.MysqlTestLifecycle
import com.twitter.finatra.mysql.client.MysqlClient
import com.twitter.finatra.mysql.util.DAOFieldExtractors._
import com.twitter.inject.Test
import com.twitter.util.Future
import com.twitter.util.Time
import com.twitter.util.mock.Mockito
import org.joda.time.DateTime

object DAOLocksTest {
  val LockName: String = "someLockName"
}

class DAOLocksTest
    extends Test
    with DAOLocks
    with DAOFieldExtractors
    with MysqlTestLifecycle
    with IgnoreMysqlHarnessIfUnavailable
    with Mockito {

  import DAOLocksTest._

  val embeddedMysqlServer: EmbeddedMysqlServer = EmbeddedMysqlServer
    .newBuilder()
    .withUsers(Seq(User.Root))
    .newServer()

  lazy val mysqlClient: MysqlClient = embeddedMysqlServer.mysqlClient
  lazy val mysqlClient2: MysqlClient = embeddedMysqlServer.mysqlClient

  case class Record(
    id: Int,
    string1: String,
    string2: Option[String],
    int1: Int,
    int2: Option[Int],
    long1: Long,
    long2: Option[Long],
    bool1: Boolean,
    bool2: Option[Boolean],
    dateTime1: DateTime,
    dateTime2: Option[DateTime])

  val string1 = "string1"
  val string2 = "string2"
  val int1 = 2147483647
  val int2: Int = -2147483648
  val long1 = 9223372036854775807L
  val long2: Long = -9223372036854775808L
  val bool1 = true
  val bool2 = false
  val dateTime1 = new DateTime(0)
  val dateTime2 = new DateTime(1000L)

  val row1Id = 1
  val row2Id = 2

  val record1: Record = Record(
    row1Id,
    string1,
    Some(string2),
    int1,
    Some(int2),
    long1,
    Some(long2),
    bool1,
    Some(bool2),
    dateTime1,
    Some(dateTime2)
  )

  val record2: Record = Record(
    row2Id,
    string1,
    None,
    int1,
    None,
    long1,
    None,
    bool1,
    None,
    dateTime1,
    None
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    val sql =
      """
      CREATE TABLE test (
        id int(10) NOT NULL AUTO_INCREMENT,
        string_required varchar(64) CHARACTER SET ascii NOT NULL,
        string_optional varchar(64) CHARACTER SET ascii NULL,
        int_required int NOT NULL,
        int_optional int NULL,
        long_required bigint NOT NULL,
        long_optional bigint NULL,
        bool_required tinyint NOT NULL,
        bool_optional tinyint NULL,
        datetime_required datetime NOT NULL,
        datetime_optional datetime NULL,
        PRIMARY KEY (id)
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8
    """
    await(mysqlClient.query(sql))
    populate
  }

  override def afterEach(): Unit = {
    super.afterEach()
    await(mysqlClient.query("drop table test"))
  }

  test("DAOLocks#Mysql db tools should select a fully populated row correctly", MysqlHarnessTag) {
    val sql = "select * from test where id=?"
    val recordSeq: Seq[Record] = await(mysqlClient.prepare(sql).select(row1Id)(convertRow))
    recordSeq.size should equal(1)
    recordSeq.head should equal(record1)

    val recordOpt = await(selectOne(mysqlClient, sql, convertRow(_), row1Id))
    recordOpt.get should equal(record1)
  }

  test("DAOLocks#Mysql db tools should select a row with nulls correctly", MysqlHarnessTag) {
    val sql = "select * from test where id=?"
    val recordSeq: Seq[Record] = await(mysqlClient.prepare(sql).select(row2Id)(convertRow))
    recordSeq.size should equal(1)
    recordSeq.head should equal(record2)

    val recordOpt = await(selectOne(mysqlClient, sql, convertRow(_), row2Id))
    recordOpt.get should equal(record2)
  }

  test("DAOLocks#Mysql db tools should select count correctly", MysqlHarnessTag) {
    var sql = "select count(*) as cnt from test where datetime_optional is null"
    var result: Long = await(selectCount(mysqlClient, sql, "cnt"))
    result should equal(1)

    sql = "select count(*) as cnt from test where id > ?"
    result = await(selectCount(mysqlClient, sql, "cnt", 1000))
    result should equal(0)
  }

  test(
    "DAOLocks#Mysql db tools should fail when extracting null with non-null extractor",
    MysqlHarnessTag) {
    val sql = "select string_optional from test where id=?"
    a[NullValueException] shouldBe thrownBy {
      // Should be calling extractStringOption
      await(selectOne(mysqlClient, sql, extractString("string_optional")(_), row2Id))
    }
  }

  test(
    "DAOLocks#Mysql db tools should fail when extracting non-null field with wrong type extractor",
    MysqlHarnessTag) {
    val sql = "select string_required from test where id=1"
    a[DataTypeException] shouldBe thrownBy {
      // Should be calling extractString
      await(selectOne(mysqlClient, sql, extractInt("string_required")(_)))
    }
  }

  test(
    "DAOLocks#Mysql db tools should fail when extracting nullable field with data using wrong type extractor",
    MysqlHarnessTag) {
    val sql = "select datetime_optional from test where id=1"
    a[DataTypeException] shouldBe thrownBy {
      // Should be calling extractDatetimeOption
      await(selectOne(mysqlClient, sql, extractLong("datetime_optional")(_)))
    }
  }

  test("DAOLocks#Mysql db tools should fail when too many rows match", MysqlHarnessTag) {
    val sql = "select * from test" // e.g. we forgot the where clause
    a[TooManyRowsException] shouldBe thrownBy {
      //noinspection ConvertibleToMethodValue
      await(selectOne(mysqlClient, sql, convertRow(_)))
    }
  }

  test("DAOLocks#Mysql db tools should fail when extracting invalid column", MysqlHarnessTag) {
    val sql = "select id from test where id=1"
    a[NoSuchColumnException] shouldBe thrownBy {
      await(selectOne(mysqlClient, sql, extractInt("ID")(_))) // it's case-sensitive!
    }
  }

  test("DAOLocks#be able to use advisory locks", MysqlHarnessTag) {
    await(getAdvisoryLock(mysqlClient, LockName))

    var boolResult = await(isAdvisoryLockFree(mysqlClient, LockName))
    boolResult shouldBe false

    await(releaseAdvisoryLock(mysqlClient, LockName))

    boolResult = await(isAdvisoryLockFree(mysqlClient, LockName))
    boolResult shouldBe true
  }

  test(
    "DAOLocks#Failed mysql results in the transaction and lock being returned but no session discarding",
    MysqlHarnessTag) {
    a[ServerError] shouldBe thrownBy {
      await {
        synchronizedTransaction(mysqlClient, LockName, Some(1)) { client =>
          client.query("invalid sql command")
        }
      }
    }
  }

  test(
    "DAOLocks#Non-MySQL errors result in releasing locks and discarding sessions",
    MysqlHarnessTag) {
    val mockMysqlClient = new MockMysqlClient
    a[RuntimeException] shouldBe thrownBy {
      await {
        synchronizedTransaction(mockMysqlClient, LockName, Some(1)) { client =>
          Future.exception(
            new RuntimeException("Something besides a mysql server error happened here"))
        }
      }
    }

    mockMysqlClient.sessionDiscarded shouldBe true
  }

  test(
    "DAOLocks#MySQL Server errors result in releasing locks and not discarding sessions",
    MysqlHarnessTag) {
    val mockMysqlClient = new MockMysqlClient
    a[ServerError] shouldBe thrownBy {
      await {
        synchronizedTransaction(mockMysqlClient, LockName, Some(1)) { client =>
          Future.exception(ServerError(0, "", ""))
        }
      }
    }

    mockMysqlClient.sessionDiscarded shouldBe false
  }

  test(
    "DAOLocks#MySQL Server errors result in releasing locks and discarding sessions if close fails",
    MysqlHarnessTag) {
    val mockMysqlClient = new MockMysqlClient
    a[ServerError] shouldBe thrownBy {
      await {
        synchronizedTransaction(mockMysqlClient, LockName, Some(1)) { client =>
          mockMysqlClient.failReleaseLock = true
          Future.exception(ServerError(0, "", ""))
        }
      }
    }

    mockMysqlClient.sessionDiscarded shouldBe true
  }

  class MockMysqlClient extends Client with Transactions with Session {
    @volatile var sessionDiscarded = false
    @volatile var failReleaseLock = false

    override def discard(): Future[Unit] = {
      sessionDiscarded = true
      Future.Done
    }

    override def session[T](f: Client with Transactions with Session => Future[T]): Future[T] = {
      f(this)
    }

    override def transaction[T](f: Client => Future[T]): Future[T] = {
      f(this)
    }

    private val rowFromAcquireLock = new Row {
      override val fields: IndexedSeq[Field] = IndexedSeq(
        Field("", "", "", "", "name", "origName", 0, 0, 0, 0, 0))
      override val values: IndexedSeq[Value] = IndexedSeq(LongValue(1))

      override def indexOf(columnName: String): Option[Int] = Some(0)
    }

    override def prepare(sql: String): PreparedStatement = new PreparedStatement {
      override def apply(params: Parameter*): Future[Result] = {
        if (failReleaseLock) {
          Future.exception(new RuntimeException(s"Some exception releasing the lock: $sql"))
        } else {
          Future.value(ResultSet(rowFromAcquireLock.fields, Seq(rowFromAcquireLock)))
        }
      }
    }

    override def select[T](sql: String)(f: Row => T): Future[Seq[T]] = ???

    override def query(sql: String): Future[Result] = ???

    override def close(deadline: Time): Future[Unit] = ???

    override def cursor(sql: String): CursoredStatement = ???

    override def ping(): Future[Unit] = ???

    override def transactionWithIsolation[T](
      isolationLevel: IsolationLevel
    )(
      f: Client => Future[T]
    ): Future[T] = ???
  }

  private def convertRow(row: Row): Record = {
    Record(
      id = extractInt("id")(row),
      string1 = extractString("string_required")(row),
      string2 = extractStringOption("string_optional")(row),
      int1 = extractInt("int_required")(row),
      int2 = extractIntOption("int_optional")(row),
      long1 = extractLong("long_required")(row),
      long2 = extractLongOption("long_optional")(row),
      bool1 = extractBoolean("bool_required")(row),
      bool2 = extractBooleanOption("bool_optional")(row),
      dateTime1 = extractDateTime("datetime_required")(row),
      dateTime2 = extractDateTimeOption("datetime_optional")(row)
    )
  }

  private def populate: Result = {
    val sql =
      """
    insert into test (id, string_required, string_optional, int_required, int_optional,
      long_required, long_optional, bool_required, bool_optional, datetime_required, datetime_optional)
    values
      (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?),
      (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """
    await(
      mysqlClient.prepare(sql)(
        record1.id,
        record1.string1,
        asParameter(record1.string2),
        record1.int1,
        asParameter(record1.int2),
        record1.long1,
        asParameter(record1.long2),
        record1.bool1,
        asParameter(record1.bool2),
        asValue(record1.dateTime1),
        asParameter(asValue(record1.dateTime2)),
        record2.id,
        record2.string1,
        asParameter(record2.string2),
        record2.int1,
        asParameter(record2.int2),
        record2.long1,
        asParameter(record2.long2),
        record2.bool1,
        asParameter(record2.bool2),
        asValue(record2.dateTime1),
        asParameter(asValue(record2.dateTime2))
      )
    )

  }
}
