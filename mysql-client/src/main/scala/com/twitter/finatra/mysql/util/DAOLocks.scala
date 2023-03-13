package com.twitter.finatra.mysql.util

import com.twitter.finagle.mysql.Client
import com.twitter.finagle.mysql.Parameter
import com.twitter.finagle.mysql.ServerError
import com.twitter.finagle.mysql.Session
import com.twitter.finagle.mysql.Transactions
import com.twitter.finagle.stats.NullStatsReceiver
import com.twitter.finagle.stats.Stat
import com.twitter.finatra.mysql.util.DAOFieldExtractors._
import com.twitter.finatra.mysql.util.DAOLocks.AdvisoryLockException
import com.twitter.util.Future
import com.twitter.util.logging.Logging
import scala.util.control.NonFatal

object DAOLocks extends DAOLocks {

  case object AdvisoryLockException extends Exception

}

trait DAOLocks extends Logging {

  def getAdvisoryLock(
    client: Client,
    lockName: String,
    timeoutSeconds: Option[Int] = None
  ): Future[Unit] = {
    // timeout of 0 means fail immediately if lock is unavailable.  -1 means wait forever
    selectOne(
      client,
      "select get_lock(?,?) as value",
      extractLong("value")(_),
      Parameter.wrap(lockName),
      Parameter.wrap(timeoutSeconds.getOrElse(0))
    ).flatMap {
      case Some(1) => Future.Unit
      case _ => Future.exception(AdvisoryLockException)
    }
  }

  def releaseAdvisoryLock(client: Client, lockName: String): Future[Unit] = {
    selectOne(client, "select release_lock(?) as value", extractLong("value")(_), lockName)
      .flatMap {
        case Some(1) => Future.Unit
        case _ =>
          error(s"Advisory lock $lockName cannot be released as it is not owned by the client")
          Future.Unit // silently ignore this.
      }
  }

  def isAdvisoryLockFree(client: Client, lockName: String): Future[Boolean] = {
    selectOne(client, "select is_free_lock(?) as value", extractLong("value")(_), lockName)
      .flatMap {
        case Some(1) => Future.value(true)
        case _ => Future.value(false)
      }
  }

  def synchronizedTransaction[A](
    mysqlClient: Client with Transactions,
    advisoryLockName: String,
    timeoutSeconds: Option[Int] = None,
    lockTimingStat: Stat = NullStatsReceiver.stat("nullStat")
  )(
    f: Client => Future[A]
  ): Future[A] = {

    def discardSession(sessionClient: Client with Transactions with Session): Future[Unit] = {
      sessionClient.discard()
    }

    def executeTransactionInSession(
      sessionClient: Client with Transactions with Session
    ): Future[A] = {
      sessionClient.transaction { sessionInTransaction =>
        f(sessionInTransaction)
          .onFailure(ex => error("Failed executing f() within transaction", ex))
      }
    }

    def releaseLock(sessionClient: Client with Transactions with Session): Future[Unit] = {
      releaseAdvisoryLock(sessionClient, advisoryLockName)
        .rescue {
          case NonFatal(ex) =>
            // If releasing the lock fails, that is 'ok' because the inner command succeeded so we can
            // return the result of the operation we actually care about, and because the release lock
            // failed, we'll discard the session to ensure everything is cleaned up with respect to the
            // lock.
            error(s"Failed releasing lock $advisoryLockName, discarding session", ex)
            discardSession(sessionClient)
        }
    }

    def executeWithinLock(
      sessionClient: Client with Transactions with Session
    )(
      opInLock: Client with Transactions with Session => Future[A]
    ): Future[A] = {
      debug(s"Attempting to get advisory lock [$advisoryLockName]")

      val resultFuture = for {
        _ <- Stat.timeFuture(lockTimingStat)(
          getAdvisoryLock(sessionClient, advisoryLockName, timeoutSeconds))
        result <- opInLock(sessionClient)
        _ <- releaseLock(sessionClient)
      } yield result

      resultFuture.rescue {
        case AdvisoryLockException =>
          // Failed to acquire the original lock, just log and propagate the exception
          debug(s"Failed to acquire advisory lock [$advisoryLockName]")
          Future.exception(AdvisoryLockException)
        case ex: ServerError =>
          error(s"MySql command failed", ex)
          releaseLock(sessionClient)
            .transform(_ => Future.exception(ex))
        case NonFatal(ex) =>
          // Either failed because:
          // - Failed to acquire lock due to network issue
          // - Failed to execute transaction/query due to network issue

          error("Failed due to non-MySql server error", ex)
          // Try and release the lock, and then discard the session regardless
          releaseLock(sessionClient)
            .flatMap(_ => discardSession(sessionClient))
            .transform(_ => Future.exception(ex))
      }
    }

    mysqlClient.session { sessionClient =>
      executeWithinLock(sessionClient) { sessionClientInLock =>
        executeTransactionInSession(sessionClientInLock)
      }
    }
  }
}
