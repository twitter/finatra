package com.twitter.finatra.http.exceptions

@deprecated(
  "Specially typing a default exception mapper is no longer necessary. Extend ExceptionMapper[Throwable] directly.",
  "2016-09-07"
)
trait DefaultExceptionMapper extends ExceptionMapper[Throwable]
