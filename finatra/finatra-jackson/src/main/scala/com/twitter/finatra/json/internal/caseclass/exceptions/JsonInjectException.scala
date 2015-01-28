package com.twitter.finatra.json.internal.caseclass.exceptions

import com.google.inject.Key

case class JsonInjectException(
  parentClass: Class[_],
  fieldName: String,
  key: Key[_],
  cause: Throwable)
  extends Exception(
    "Unable to inject field '" + fieldName + "' with " + key +
      " into class " + parentClass, cause)
