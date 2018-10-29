package com.twitter.finatra.json.internal.caseclass.exceptions

import com.fasterxml.jackson.databind.JsonMappingException

/**
 * Exception for handling Finatra-specific errors that may otherwise be valid
 * in the eyes of Jackson.
 *
 * @note the given [[msg]] generally passes all the way up the stack to the caller of
 *       the method throwing this Exception, thus care should be taken to not leak any
 *       internal implementation details.
 *
 * @param msg exception message.
 */
class FinatraJsonMappingException(msg: String) extends JsonMappingException(null, msg)
