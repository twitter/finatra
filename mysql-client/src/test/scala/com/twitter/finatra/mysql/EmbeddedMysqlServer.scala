package com.twitter.finatra.mysql

import com.twitter.conversions.DurationOps.richDurationFromInt
import com.twitter.finagle.Mysql
import com.twitter.finagle.mysql.ResultSet
import com.twitter.finagle.mysql.StringValue
import com.twitter.finagle.mysql.harness.EmbeddedDatabase
import com.twitter.finagle.mysql.harness.EmbeddedInstance
import com.twitter.finagle.mysql.harness.MySqlExecutables
import com.twitter.finagle.mysql.harness.config.DatabaseConfig
import com.twitter.finagle.mysql.harness.config.InstanceConfig
import com.twitter.finagle.mysql.harness.config.MySqlVersion
import com.twitter.finagle.mysql.harness.config.User
import com.twitter.finagle.stats.NullStatsReceiver
import com.twitter.finatra.mysql.client.MysqlClient
import com.twitter.util.logging.Logging
import com.twitter.util.Await
import com.twitter.util.Duration
import com.twitter.util.Future
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import org.scalatest.matchers.should.Matchers
import scala.util.control.NonFatal

object EmbeddedMysqlServer {
  // example placeholder for test harness MySqlVersion. TODO - replace with public defaults
  private[twitter] val v5_7_28 = MySqlVersion(
    5,
    7,
    28,
    Map(
      "--innodb_use_native_aio" -> "0",
      "--innodb_stats_persistent" -> "0",
      "--innodb_strict_mode" -> "1",
      "--explicit_defaults_for_timestamp" -> "0",
      "--sql_mode" -> "NO_ENGINE_SUBSTITUTION",
      "--character_set_server" -> "utf8",
      "--default_time_zone" -> "+0:00",
      "--innodb_file_format" -> "Barracuda",
      "--enforce_gtid_consistency" -> "ON",
      "--log-bin" -> "binfile",
      "--server-id" -> "1"
    )
  )

  // example placeholder for test harness MySqlVersion. TODO - replace with public defaults
  private[twitter] val v8_0_21: MySqlVersion = MySqlVersion(
    8,
    0,
    21,
    Map(
      "--default_storage_engine" -> "InnoDB",
      "--character_set_server" -> "utf8",
      "--collation_server" -> "utf8_general_ci",
      "--auto_increment_increment" -> "1",
      "--auto_increment_offset" -> "2",
      "--transaction_isolation" -> "REPEATABLE-READ",
      "--innodb_autoinc_lock_mode" -> "1",
      "--group_concat_max_len" -> "1024",
      "--explicit_defaults_for_timestamp" -> "0",
      "--gtid_mode" -> "OFF",
      "--enforce-gtid-consistency" -> "WARN",
      "--sql_mode" -> "NO_ENGINE_SUBSTITUTION",
      "--default_time_zone" -> "+0:00",
      "--log-bin" -> "binfile",
      "--server-id" -> "1",
      "--mysqlx" -> "OFF"
    )
  )

  case class Config private (
    baseClient: Mysql.Client = Mysql.client,
    version: MySqlVersion = v5_7_28, // TODO - replace with public default before SBT build support
    path: Path = Paths.get(System.getProperty("java.io.tmpdir")),
    databaseName: String = "a_database",
    users: Seq[User] = Seq.empty,
    setupQueries: Seq[String] = Seq.empty,
    closeGracePeriod: Duration = 1.second,
    closeAwaitPeriod: Duration = 2.seconds) {
    def withBaseClient(baseClient: Mysql.Client): Config = copy(baseClient = baseClient)
    def withVersion(version: MySqlVersion): Config = copy(version = version)
    def withPath(path: Path): Config = copy(path = path)
    def withDatabaseName(databaseName: String): Config = copy(databaseName = databaseName)
    def withUsers(users: Seq[User]): Config = copy(users = users)
    def withSetupQueries(setupQueries: Seq[String]): Config = copy(setupQueries = setupQueries)
    def withCloseGracePeriod(closeGracePeriod: Duration): Config =
      copy(closeGracePeriod = closeGracePeriod)
    def withCloseAwaitPeriod(closeAwaitPeriod: Duration): Config =
      copy(closeAwaitPeriod = closeAwaitPeriod)

    /** Generate the new [[EmbeddedMysqlServer]] from the configured properties */
    def newServer(): EmbeddedMysqlServer = new EmbeddedMysqlServer(
      version = version,
      path = path,
      databaseName = databaseName,
      users = users,
      setupQueries = setupQueries,
      closeGracePeriod = closeGracePeriod,
      closeAwaitPeriod = closeAwaitPeriod,
      baseClient
    )
  }

  /**
   * Create a new [[EmbeddedMysqlServer]] builder configuration.
   *
   * @example {{{
   *            val server: EmbeddedMysqlServer = EmbeddedMysqlServer
   *              .newBuilder
   *              .withDatabaseName("mydb")
   *              .newServer()
   * }}}
   */
  def newBuilder(): Config = Config()

  /**
   * Create a new [[EmbeddedMysqlServer]] with the default settings. This is the equivalent
   * of calling {{{ EmbeddedMysqlServer.newBuilder.newServer }}}.
   */
  def apply(): EmbeddedMysqlServer = newBuilder().newServer()

}

/**
 * The [[EmbeddedMysqlServer]] acts as a facade for the finagle-mysql [[EmbeddedDatabase]] and
 * exposes lifecycle concerns in a manner more consistent with Finatra's [[com.twitter.inject.server.EmbeddedTwitterServer]].
 *
 * @param version The [[MySqlVersion]] to use.
 * @param path The [[Path]] where the MySQL binaries exist.
 * @param databaseName The name of the database to be initialized.
 * @param users The [[User user]] credentials to assign to the underlying database instance.
 * @param setupQueries Queries that will be run upon database initialization, which can be used
 *                     for purposes such as creating or populating tables within the database.
 * @param closeGracePeriod The amount of time given to gracefully close the server's clients
 *                         (note that this is advisory).
 * @param closeAwaitPeriod The maximum amount of time to wait for the server's clients to close
 *                         gracefully.
 *
 * @note The behavior of the [[EmbeddedMysqlServer]] is tightly coupled with the finagle-mysql
 *       test utilities and may not be appropriate for all runtime environments.
 */
final class EmbeddedMysqlServer private (
  version: MySqlVersion,
  path: Path,
  databaseName: String,
  users: Seq[User],
  setupQueries: Seq[String],
  closeGracePeriod: Duration,
  closeAwaitPeriod: Duration,
  baseClient: Mysql.Client)
    extends Logging
    with Matchers {

  private[this] val exits: ConcurrentLinkedQueue[() => Unit] =
    new ConcurrentLinkedQueue[() => Unit]()

  @volatile private[this] var _database: EmbeddedDatabase = _
  @volatile private[this] var _instance: EmbeddedInstance = _
  private[this] val _starting: AtomicBoolean = new AtomicBoolean(false)
  private[this] val _started: AtomicBoolean = new AtomicBoolean(false)
  private[this] val _closed: AtomicBoolean = new AtomicBoolean(false)
  @volatile var _error: Option[Throwable] = None

  def testHarnessExists(): Boolean = MySqlExecutables.fromPath(path).isDefined

  def start(): Unit = {
    throwIfStartupFailed()

    if (isClosed())
      throw new IllegalStateException(
        s"EmbeddedMysqlServer '$databaseName' has already been closed")

    _started synchronized {
      // we double check if we're closed in case of a race (unlikely)
      if (isClosed())
        throw new IllegalStateException(
          s"EmbeddedMysqlServer '$databaseName' has already been closed")

      if (_starting.compareAndSet(false, true)) try {
        EmbeddedInstance.getInstance(InstanceConfig(version, path)) match {
          case None =>
            throw new IllegalStateException(
              s"No Mysql harness available for EmbeddedMysqlServer '$databaseName' with version '$version' at path '$path'")
          case Some(instance) =>
            val databaseConfig =
              DatabaseConfig(
                databaseName = databaseName,
                users = users,
                setupQueries = setupQueries)
            _instance = instance
            _database = EmbeddedDatabase.getInstance(databaseConfig, _instance)
            _database.init()
            _started.set(true)
        }
      } catch {
        case NonFatal(t) =>
          _error = Some(t)
          throwIfStartupFailed()
      }
    }
  }

  private[this] def throwIfStartupFailed(): Unit = _error match {
    case Some(t) => throw t
    case _ => ()
  }

  def isStarted(): Boolean = _started.get()

  def isClosed(): Boolean = _closed.get()

  def assertStarted(started: Boolean = true): Unit = assert(_started.get() == started)

  def close(): Unit =
    if (_closed.compareAndSet(false, true)) {
      _started synchronized {
        while (!exits.isEmpty) {
          val exitFn = exits.poll()
          try {
            exitFn()
          } catch {
            case NonFatal(e) =>
              // we swallow close exceptions, but print them out for debug purposes
              new RuntimeException(
                s"Error closing MysqlClient for EmbeddedMysqlServer '$databaseName'",
                e)
                .printStackTrace(System.err)
          }
        }
      }
    }

  /** The base [[MysqlClient]] to the underlying EmbeddedMysql instance, which uses the Root User */
  lazy val mysqlClient: MysqlClient = mysqlClient(User.Root)

  /** The base [[MysqlClient]] to the underlying EmbeddedMysql instance */
  def mysqlClient(user: User): MysqlClient = {
    start()
    debug(s"Creating client for '$user' on EmbeddedMysqlServer '$databaseName'")
    val underlying = baseClient
      .withStatsReceiver(NullStatsReceiver)
      .withDatabase(databaseName)
      .withCredentials(user.name, user.password.orNull)
      .newRichClient(_instance.dest, s"$databaseName-${user.name}")
    val client = new MysqlClient(underlying)
    exits.add(() => close(user, client))
    client
  }

  /**
   * Execute a MySQL `TRUNCATE` command for all tables in this server's database. Tables that should
   * be skipped/preserved can be specified. Please use caution when executing this command.
   *
   * @param preserveTables Tables that will not be truncated from this server's database.
   * @param timeout The amount of time to await for the truncation to complete.
   */
  def truncateAllTables(
    preserveTables: Seq[String] = Seq.empty,
    timeout: Duration = 30.seconds
  ): Unit = {
    val tables: Seq[String] = extractTables()
      .filterNot(preserveTables.contains)

    val truncateFuture = mysqlClient.session { clnt =>
      // setting foreign_key_checks is per session
      val truncate: Future[Unit] = clnt.query("set foreign_key_checks=0").flatMap { _ =>
        Future
          .collect(tables.map { t => clnt.query(s"TRUNCATE $t;") }).unit
      }

      truncate.ensure { clnt.query("set foreign_key_checks=1") }
    }

    Await.result(truncateFuture, timeout)
  }

  private[this] lazy val selectTablesWithNonZeroRowsQuery: String =
    s"""
       |SELECT TABLE_NAME
       |FROM INFORMATION_SCHEMA.TABLES
       |WHERE TABLE_SCHEMA = '$databaseName'
       |AND TABLE_ROWS > 0
      """.stripMargin

  private[this] def extractTables(timeout: Duration = 30.seconds): Seq[String] = {
    val result = Await.result(mysqlClient.query(selectTablesWithNonZeroRowsQuery), timeout)
    result match {
      case ResultSet(_, rows) =>
        rows.flatMap(_.values).flatMap {
          case StringValue(s) => Some(s)
          case _ => None
        }
      case unknown =>
        throw new RuntimeException(s"Unexpected result: $unknown")
    }
  }

  private[this] def close(user: User, client: MysqlClient): Unit = {
    trace(s"EmbeddedMysqlServer '$databaseName' user '$user' close is being initiated...")
    Await.result(client.close(closeGracePeriod), closeAwaitPeriod)
    trace(s"EmbeddedMysqlServer '$databaseName' user '$user' is closed.")
  }
}
