namespace java com.twitter.adder.thriftjava
#@namespace scala com.twitter.adder.thriftscala
namespace rb Adder

include "finatra-thrift/finatra_thrift_exceptions.thrift"

service Adder {

  i32 add1(
    1: i32 num
  ) throws (
    1: finatra_thrift_exceptions.ServerError serverError,
    2: finatra_thrift_exceptions.UnknownClientIdError unknownClientIdError
    3: finatra_thrift_exceptions.NoClientIdError noClientIdError
  )

  string add1String(
    1: string num
  ) throws (
    1: finatra_thrift_exceptions.ServerError serverError,
    2: finatra_thrift_exceptions.UnknownClientIdError unknownClientIdError
    3: finatra_thrift_exceptions.NoClientIdError noClientIdError
  )

  string add1Slowly(
    1: string num
  ) throws (
    1: finatra_thrift_exceptions.ServerError serverError,
    2: finatra_thrift_exceptions.UnknownClientIdError unknownClientIdError
    3: finatra_thrift_exceptions.NoClientIdError noClientIdError
  )

  string add1AlwaysError(
    1: string num
  )
}
