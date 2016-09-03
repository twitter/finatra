namespace java com.twitter.calculator.thriftjava
#@namespace scala com.twitter.calculator.thriftscala
namespace rb Calculator

include "finatra-thrift/finatra_thrift_exceptions.thrift"

service Calculator {

  /**
   * Increment a number
   */
  i32 increment(
    1: i32 a
  ) throws (
    1: finatra_thrift_exceptions.ServerError serverError,
    2: finatra_thrift_exceptions.UnknownClientIdError unknownClientIdError
    3: finatra_thrift_exceptions.NoClientIdError noClientIdError
  )

  /**
   * Add two numbers
   */
  i32 addNumbers(
    1: i32 a
    2: i32 b
  ) throws (
    1: finatra_thrift_exceptions.ServerError serverError,
    2: finatra_thrift_exceptions.UnknownClientIdError unknownClientIdError
    3: finatra_thrift_exceptions.NoClientIdError noClientIdError
  )

  /**
   * Add two strings
   */
  string addStrings(
    1: string a
    2: string b
  ) throws (
    1: finatra_thrift_exceptions.ClientError clientError,
    2: finatra_thrift_exceptions.ServerError serverError,
    3: finatra_thrift_exceptions.UnknownClientIdError unknownClientIdError
    4: finatra_thrift_exceptions.NoClientIdError noClientIdError
  )
}
