namespace java com.twitter.finatra.thrift.thriftjava
namespace py gen.twitter.finatra.thrift.thriftpy
#@namespace scala com.twitter.finatra.thrift.thriftscala
#@namespace strato com.twitter.finatra.thrift
namespace rb FinatraThrift
# Ending a golang namespace with `thrift` breaks thrift compilation
namespace go finatra.thriftgo

exception UnknownClientIdError {
  1: string message
}

exception NoClientIdError {
  1: string message
}

enum ClientErrorCause {
  /** Improperly-formatted request can't be fulfilled. */
  BAD_REQUEST     = 0,

  /** Required request authorization failed. */
  UNAUTHORIZED    = 1,

  /** Server timed out while fulfilling the request. */
  REQUEST_TIMEOUT = 2,

  /** Initiating client has exceeded its maximum rate. */
  RATE_LIMITED    = 3
}

exception ClientError {
  1: ClientErrorCause errorCause
  2: string message
}

enum ServerErrorCause {
  /** Generic server error. */
  INTERNAL_SERVER_ERROR = 0,

  /** Server lacks the ability to fulfill the request. */
  NOT_IMPLEMENTED       = 1,

  /** Request cannot be fulfilled due to error from dependent service. */
  DEPENDENCY_ERROR      = 2,

  /** Server is currently unavailable. */
  SERVICE_UNAVAILABLE   = 3
}

exception ServerError {
  1: ServerErrorCause errorCause
  2: string message
}

/**
 * To generate Exceptions, scrooge uses finagle-core, which pants does not provide
 * to thrift libraries which do not define services (structs-only definitions generally
 * do not need finagle). Therefore, we define an empty service to cause the finagle-core
 * dep to be introduced.
 *
 * see DPB-7778
 */
service EmptyExceptionsService { }
