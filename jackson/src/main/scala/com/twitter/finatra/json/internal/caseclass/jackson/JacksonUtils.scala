package com.twitter.finatra.json.internal.caseclass.jackson

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonMappingException

object JacksonUtils {

  /* Public */
  def errorMessage(e: JsonProcessingException): String = e match {
    case jme: JsonMappingException if isLeaky(jme) =>
      "Unable to parse" // Prevent JsonMappingException from leaking implementation details
    case _ if e.getCause == null =>
      e.getOriginalMessage // jackson threw the original error
    case _ =>
      e.getCause.getMessage // custom deserialization code threw the exception (e.g., enum deserialization)
  }

  /* Private */

  private def isLeaky(e: JsonProcessingException) = {
    val msg = e.getMessage
    if (msg.startsWith("field cannot be"))
      false
    else if (msg.startsWith("error parsing"))
      false
    else
      true
  }
}
