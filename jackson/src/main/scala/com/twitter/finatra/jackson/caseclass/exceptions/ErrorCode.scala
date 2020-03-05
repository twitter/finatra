package com.twitter.finatra.jackson.caseclass.exceptions

import com.fasterxml.jackson.core.JsonProcessingException
import com.twitter.finatra.validation.{ErrorCode => ValidationErrorCode}

object ErrorCode {
  case class JsonProcessingError(cause: JsonProcessingException) extends ValidationErrorCode
}
