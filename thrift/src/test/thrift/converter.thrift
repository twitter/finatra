namespace java com.twitter.converter.thriftjava
#@namespace scala com.twitter.converter.thriftscala
namespace rb Converter

include "finatra-thrift/finatra_thrift_exceptions.thrift"

service Converter {

  string uppercase(
    1: string msg
  ) throws (
    1: finatra_thrift_exceptions.ServerError serverError,
    2: finatra_thrift_exceptions.UnknownClientIdError unknownClientIdError
    3: finatra_thrift_exceptions.NoClientIdError noClientIdError
  )

}
