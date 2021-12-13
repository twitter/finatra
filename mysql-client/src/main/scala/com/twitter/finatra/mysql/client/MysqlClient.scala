package com.twitter.finatra.mysql.client

import com.twitter.finagle.mysql.Client
import com.twitter.finagle.mysql.CursoredStatement
import com.twitter.finagle.mysql.IsolationLevel
import com.twitter.finagle.mysql.PreparedStatement
import com.twitter.finagle.mysql.Result
import com.twitter.finagle.mysql.Row
import com.twitter.finagle.mysql.Session
import com.twitter.finagle.mysql.Transactions
import com.twitter.util.ClosableOnce
import com.twitter.util.Future
import com.twitter.util.Time

object MysqlClient {
  def apply(underlying: Client with Transactions): MysqlClient = new MysqlClient(underlying)
}

/**
 * A wrapper for Finagle MySQL's [[Client with Transactions]] which eases injection and binding.
 *
 * @see [[com.twitter.finatra.mysql.client.modules.MysqlClientModuleTrait]]
 */
class MysqlClient(val self: Client with Transactions)
    extends Client
    with Transactions
    with ClosableOnce
    with Proxy.Typed[Client with Transactions] {

  /** @inheritdoc */
  def query(sql: String): Future[Result] = self.query(sql)

  /** @inheritdoc */
  def select[T](sql: String)(f: Row => T): Future[Seq[T]] = self.select(sql)(f)

  /** @inheritdoc */
  def prepare(sql: String): PreparedStatement = self.prepare(sql)

  /** @inheritdoc */
  def cursor(sql: String): CursoredStatement = self.cursor(sql)

  /** @inheritdoc */
  def ping(): Future[Unit] = self.ping()

  /** @inheritdoc */
  def session[T](f: Client with Transactions with Session => Future[T]): Future[T] =
    self.session(f)

  /** @inheritdoc */
  def transaction[T](f: Client => Future[T]): Future[T] = self.transaction(f)

  /** @inheritdoc */
  def transactionWithIsolation[T](
    isolationLevel: IsolationLevel
  )(
    f: Client => Future[T]
  ): Future[T] =
    self.transactionWithIsolation(isolationLevel)(f)

  /** @inheritdoc */
  def closeOnce(deadline: Time): Future[Unit] = self.close(deadline)
}
