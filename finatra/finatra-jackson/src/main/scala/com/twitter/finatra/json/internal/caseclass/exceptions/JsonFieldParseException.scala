package com.twitter.finatra.json.internal.caseclass.exceptions

import com.twitter.finatra.json.internal.caseclass.CaseClassField

case class JsonFieldParseException(
  msg: String)
  extends Exception(msg) {

  def nestFieldName(field: CaseClassField, methodValidation: Boolean = false) = {
    if(methodValidation)
      formatExceptionMessage(field, ": ")
    else
      formatExceptionMessage(field, ".")
  }

  private def formatExceptionMessage(field: CaseClassField, fieldSeparator: String) = {
    JsonFieldParseException(field.name + fieldSeparator + msg)
  }
}
