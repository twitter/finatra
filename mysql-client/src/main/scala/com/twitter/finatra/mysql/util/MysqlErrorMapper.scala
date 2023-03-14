package com.twitter.finatra.mysql.util

object MysqlErrorMapper {
  def apply(errorCode: Short): Option[MysqlError] = errorCode match {
    case DuplicateEntry(error) => Some(error)
    case RowIsReferenced(error) => Some(error)
    case ReferencedRowNotFound(error) => Some(error)
    case LockWaitTimeoutExceeded(error) => Some(error)
    case CommandDeniedToUser(error) => Some(error)
    case TransactionDeadlock(error) => Some(error)
    case MaxPreparedStatements(error) => Some(error)
    case OptionPreventsStatement(error) => Some(error)
    case _ => None
  }

  sealed trait MysqlError {
    val errorCodes: Seq[Short]
    val metricName: String

    def unapply(errorCode: Short): Option[MysqlError] = {
      if (errorCodes.contains(errorCode))
        Some(this)
      else
        None
    }
  }

  case object LockWaitTimeoutExceeded extends MysqlError {
    override val metricName: String = "LockWaitTimeoutExceeded"

    // Matches when trying to acquire a mysql database lock, but times out
    override val errorCodes: Seq[Short] = List(1205)
  }

  case object DuplicateEntry extends MysqlError {
    override val metricName: String = "DuplicateEntry"
    // Matches when a unique key is violated
    override val errorCodes: Seq[Short] = List(1062)
  }

  case object RowIsReferenced extends MysqlError {
    override val metricName: String = "RowIsReferenced"

    // Matches when you try to delete a parent row that has children. Delete the children first.
    override val errorCodes: Seq[Short] = Seq(1217, 1451)
  }

  case object ReferencedRowNotFound extends MysqlError {
    override val metricName: String = "ReferencedRowNotFound"

    // Matches when you try to add a row but there is no parent row. Add the parent row first
    override val errorCodes: Seq[Short] = Seq(1216, 1452)
  }

  case object CommandDeniedToUser extends MysqlError {
    override val metricName: String = "CommandDeniedToUser"

    // Matches when a user is not authorized to perform an operation in the DB, e.g. RO user attempting UPDATE
    override val errorCodes: Seq[Short] = List(1142)
  }

  case object MaxPreparedStatements extends MysqlError {
    override val metricName: String = "MaxPreparedStatements"

    override val errorCodes: Seq[Short] = List(1461)
  }

  case object TransactionDeadlock extends MysqlError {
    override val metricName: String = "TransactionDeadlock"

    // Occurs when a deadlock is detected within a transaction:
    // https://dev.mysql.com/doc/refman/5.7/en/innodb-deadlocks-handling.html
    override val errorCodes: Seq[Short] = List(1213)
  }

  case object OptionPreventsStatement extends MysqlError {
    override val metricName: String = "OptionPreventsStatement"

    // Error number: 1290; Symbol: ER_OPTION_PREVENTS_STATEMENT; SQLSTATE: HY000
    // Message: The MySQL server is running with the %s option so it cannot execute this statement
    // Multiple meanings (not definitive):
    // "server is running with the --read-only option so it cannot execute this statement"
    // "server is running with the –secure-file-priv option so it cannot execute this statement"
    override val errorCodes: Seq[Short] = List(1290)
  }

}
