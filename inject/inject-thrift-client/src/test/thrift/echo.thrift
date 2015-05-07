namespace java com.twitter.test.thriftjava
#@namespace scala com.twitter.test.thriftscala
namespace rb EchoService

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

service EchoService {

  /**
   * Echo service
   */
  string echo(
    1: string msg
  ) throws (
    1: ClientError clientError,
    2: ServerError serverError
  )

  /**
   * Set message
   */
  i32 setTimesToEcho(
    1: i32 times
  ) throws (
    1: ClientError clientError,
    2: ServerError serverError
  )
}
