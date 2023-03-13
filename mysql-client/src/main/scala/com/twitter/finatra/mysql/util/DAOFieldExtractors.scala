package com.twitter.finatra.mysql.util

import com.twitter.finagle.mysql.Parameter.NullParameter
import com.twitter.finagle.mysql._
import com.twitter.util.Future
import com.twitter.util.Return
import com.twitter.util.Throw
import com.twitter.util.Try
import com.twitter.util.{Time => TwitterTime}
import java.sql.Timestamp
import java.util.TimeZone
import org.joda.time.DateTime

object DAOFieldExtractors extends DAOFieldExtractors {

  case class NoSuchColumnException(expression: String)
      extends Exception(s"Column [$expression] not found in result set")

  case class NullValueException(expression: String)
      extends Exception(s"NULL was unexpectedly returned from query for $expression")

  case class DataTypeException(expression: String, expected: String, actual: String = "")
      extends Exception(s"Wrong type found for $expression: expected $expected, actual $actual")

  case class NoRowsException() extends Exception("Query returned no rows")

  case class TooManyRowsException() extends Exception("Query returned too many rows")

  case class RowCountAndMaxUpdateId(rowCount: Long, maxId: Option[Long])

}

trait DAOFieldExtractors {

  import DAOFieldExtractors._

  def selectCount(
    client: Client,
    sql: String,
    expression: String,
    params: Parameter*
  ): Future[Long] = {
    selectCount(client.prepare(sql), expression, params: _*)
  }

  def selectCount(stmt: PreparedStatement, expression: String, params: Parameter*): Future[Long] = {
    selectOne[Long](stmt, (row: Row) => extractLong(expression)(row), params: _*).flatMap {
      case Some(cnt) => Future.value(cnt)
      case _ => Future.exception(NoRowsException())
    }
  }

  // Run a query that returns None for no rows found, or throws TooManyRowsException for more than 1 row.
  def selectOne[T](
    client: Client,
    sql: String,
    f: (Row => T),
    params: Parameter*
  ): Future[Option[T]] = {
    selectOne(client.prepare(sql), f, params: _*)
  }

  def selectOne[T](
    stmt: PreparedStatement,
    f: (Row => T),
    params: Parameter*
  ): Future[Option[T]] = {
    stmt
      .select(params: _*)(f)
      .flatMap {
        case head +: Nil => Future.value(Some(head))
        case Nil => Future.None
        case _ => Future.exception(TooManyRowsException())
      }
  }

  // Run a query that returns None for no rows found, or throws TooManyRowsException for more than 1 row.
  // The row extractor function returns an Option, so this method is handy if you are selecting
  // a value that may be legally be NULL.
  def selectOneOption[T](
    client: Client,
    sql: String,
    f: (Row => Option[T]),
    params: Parameter*
  ): Future[Option[T]] = {
    selectOneOption(client.prepare(sql), f, params: _*)
  }

  def selectOneOption[T](
    stmt: PreparedStatement,
    f: (Row => Option[T]),
    params: Parameter*
  ): Future[Option[T]] = {
    stmt
      .select(params: _*)(f)
      .flatMap {
        case head +: Nil => Future.value(head)
        case Nil => Future.None
        case _ => Future.exception(TooManyRowsException())
      }
  }

  def asValue(maybeDateTime: Option[DateTime]): Option[Value] = {
    maybeDateTime match {
      case Some(dateTime) => Some(asValue(dateTime))
      case None => None
    }
  }

  def asValue(dateTime: DateTime): Value = {
    timestampValueWithTimezone.apply(new Timestamp(dateTime.getMillis))
  }

  def asParameter[T](option: Option[T]): Parameter = {
    option match {
      case Some(t) => Parameter.unsafeWrap(t)
      case None => NullParameter
    }
  }

  def extractString(expression: String)(row: Row): String = {
    extractStringOption(expression)(row) match {
      case Some(s) => s
      case None => throw NullValueException(expression)
    }
  }

  def extractStringOption(expression: String)(row: Row): Option[String] =
    extract[String](row, expression)({
      case StringValue(s) => s
      case EmptyValue => ""
      case other => throw DataTypeException(expression, "StringValue or EmptyValue", other.toString)
    })

  def extractInt(expression: String)(row: Row): Int = {
    extractIntOption(expression)(row) match {
      case Some(i) => i
      case None => throw NullValueException(expression)
    }
  }

  def extractIntOption(expression: String)(row: Row): Option[Int] =
    extract[Int](row, expression)({
      case IntValue(i) => i
      case other => throw DataTypeException(expression, "IntValue", other.toString)
    })

  def extractLong(expression: String)(row: Row): Long = {
    extractLongOption(expression)(row) match {
      case Some(l) => l
      case None => throw NullValueException(expression)
    }
  }

  def extractLongOption(expression: String)(row: Row): Option[Long] =
    extract[Long](row, expression)({
      case LongValue(l) => l
      case other => throw DataTypeException(expression, "LongValue", other.toString)
    })

  def extractBoolean(expression: String)(row: Row): Boolean = {
    // Even tho it's a TINYINT in the database, finagle mysql is returning a Byte instead of an Int
    extractByte(expression)(row) == 1
  }

  def extractBooleanOption(expression: String)(row: Row): Option[Boolean] = {
    // Even tho it's a TINYINT in the database, finagle mysql is returning a Byte instead of an Int
    extractByteOption(expression)(row) match {
      case Some(b) => Some(b == 1)
      case None => None
    }
  }

  def extractByte(expression: String)(row: Row): Byte = {
    extractByteOption(expression)(row) match {
      case Some(b) => b
      case None => throw NullValueException(expression)
    }
  }

  def extractByteOption(expression: String)(row: Row): Option[Byte] =
    extract[Byte](row, expression)({
      case ByteValue(b) => b
      case other => throw DataTypeException(expression, "ByteValue", other.toString)
    })

  // Note: Returned DateTime will be in UTC
  def extractDateTime(expression: String)(row: Row): DateTime = {
    extractDateTimeOption(expression)(row) match {
      case Some(dateTime) => dateTime
      case None => throw NullValueException(expression)
    }
  }

  // Note: Returned DateTime will be in UTC
  def extractDateTimeOption(expression: String)(row: Row): Option[DateTime] =
    extract[DateTime](row, expression)({ v =>
      {
        timestampValueWithTimezone.unapply(v) match {
          case Some(timestamp) => new DateTime(timestamp.getTime)
          case None => throw DataTypeException(expression, "TimestampValue", "None")
        }
      }
    })

  def extractTwitterDateTime(expression: String)(row: Row): TwitterTime = {
    extractTwitterDateTimeOption(expression)(row) match {
      case Some(dateTime) => dateTime
      case None => throw NullValueException(expression)
    }
  }

  def extractTwitterDateTimeOption(expression: String)(row: Row): Option[TwitterTime] =
    extract[TwitterTime](row, expression)({ v =>
      {
        timestampValueWithTimezone.unapply(v) match {
          case Some(timestamp: java.sql.Timestamp) =>
            TwitterTime(new java.util.Date(timestamp.getTime))
          case None => throw DataTypeException(expression, "TimestampValue", "None")
        }
      }
    })

  def extractInsertId(result: Result): Long = {
    extractOk(result, _.insertId).apply()
  }

  def extractAffectedRows(result: Result): Long = {
    extractOk(result, _.affectedRows).apply()
  }

  def extractOk[A](result: Result, f: OK => A): Try[A] = {
    result match {
      case ok: OK => new Return(f(ok))
      case e: Error => new Throw(new Exception(e.message))
      case unknown =>
        new Throw(new Exception(s"Unexpected response from Result match of type $unknown"))
    }
  }

  def extractResultSet[A](result: Result, f: ResultSet => A): Try[A] = {
    result match {
      case resultSet: ResultSet => Return(f(resultSet))
      case e: Error => Throw(new Exception(e.message))
      case unknown =>
        Throw(new Exception(s"Unexpected response from Result match of type $unknown"))
    }
  }

  def extractOkWithId[A](result: Result, f: OK => (A, Long)): Try[(A, Long)] = {
    result match {
      case ok: OK => new Return(f(ok))
      case e: Error => new Throw(new Exception(e.message))
      case unknown =>
        new Throw(new Exception(s"Unexpected response from Result match of type $unknown"))
    }
  }

  def extractAffectedRowsAsFuture(result: Result): Future[Long] = {
    Future.const(extractOk(result, _.affectedRows))
  }

  def extractAffectedRowsWithIdsAsFuture(result: Result): Future[RowCountAndMaxUpdateId] = {
    Future.const(extractOkWithId(result, { ok => (ok.affectedRows, ok.insertId) }).map { i =>
      RowCountAndMaxUpdateId(i._1, Some(i._2))
    })
  }

  private def extract[T](row: Row, expression: String)(f: Value => T): Option[T] = {
    row(expression) match {
      case None => throw NoSuchColumnException(expression)
      case Some(NullValue) => None
      case Some(value) => Some(f(value))
    }
  }

  private def timestampValueWithTimezone =
    new TimestampValue(
      injectionTimeZone = TimeZone.getDefault,
      extractionTimeZone = TimeZone.getTimeZone("UTC")
    )

}
