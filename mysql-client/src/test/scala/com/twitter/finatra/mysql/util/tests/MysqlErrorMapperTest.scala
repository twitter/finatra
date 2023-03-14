package com.twitter.finatra.mysql.util

import com.twitter.finatra.mysql.util.MysqlErrorMapper._
import com.twitter.inject.Test
import org.scalatest.Matchers

object MysqlErrorMapperTest {
  private val MysqlDuplicateEntryErrorCode: Short = 1062
  private val MysqlRowIsReferencedErrorCodeOne: Short = 1217
  private val MysqlRowIsReferencedErrorCodeTwo: Short = 1451
  private val MysqlReferencedRowNotFoundErrorCode: Short = 1216
  private val MysqlCommandDeniedToUser: Short = 1142
  private val MysqlLockWaitTimeoutExceeded: Short = 1205
  private val MysqlTransactionDeadlock: Short = 1213
  private val MysqlMaxPreparedStatements: Short = 1461
  private val MysqlUnknownErrorCode: Short = -1
  private val MysqlOptionPreventsStatement: Short = 1290
}

class MysqlErrorMapperTest extends Test with Matchers {
  import MysqlErrorMapperTest._

  test("create case objects from known exception types") {
    MysqlErrorMapper(MysqlDuplicateEntryErrorCode) shouldBe Some(DuplicateEntry)
    MysqlErrorMapper(MysqlRowIsReferencedErrorCodeOne) shouldBe Some(RowIsReferenced)
    MysqlErrorMapper(MysqlReferencedRowNotFoundErrorCode) shouldBe Some(ReferencedRowNotFound)
    MysqlErrorMapper(MysqlCommandDeniedToUser) shouldBe Some(CommandDeniedToUser)
    MysqlErrorMapper(MysqlLockWaitTimeoutExceeded) shouldBe Some(LockWaitTimeoutExceeded)
    MysqlErrorMapper(MysqlTransactionDeadlock) shouldBe Some(TransactionDeadlock)
    MysqlErrorMapper(MysqlMaxPreparedStatements) shouldBe Some(MaxPreparedStatements)
    MysqlErrorMapper(MysqlOptionPreventsStatement) shouldBe Some(OptionPreventsStatement)

  }

  test("work when a single error type corresponds to multiple error codes") {
    MysqlErrorMapper(MysqlRowIsReferencedErrorCodeOne) shouldBe Some(RowIsReferenced)
    MysqlErrorMapper(MysqlRowIsReferencedErrorCodeTwo) shouldBe Some(RowIsReferenced)
  }

  test("return None when an unknown error code is passed in") {
    MysqlErrorMapper(MysqlUnknownErrorCode)
  }
}
