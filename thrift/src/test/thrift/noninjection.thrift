namespace java com.twitter.noninjection.thriftjava
#@namespace scala com.twitter.noninjection.thriftscala
namespace rb Noninjection

include "finatra-thrift/finatra_thrift_exceptions.thrift"

/** Test service for ensuring we can bring a server up without injection. */
service NonInjectionService {
  string echo(
    1: string msg
  ) throws (
    1: finatra_thrift_exceptions.ServerError serverError
    2: finatra_thrift_exceptions.UnknownClientIdError unknownClientIdError
    3: finatra_thrift_exceptions.NoClientIdError noClientIdError
  )
}
