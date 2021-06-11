package com.twitter.finatra.jackson.caseclass.exceptions

import com.google.inject.Key
import com.twitter.util.jackson.annotation.InjectableValue
import com.twitter.util.jackson.caseclass.exceptions.{
  InjectableValuesException => JacksonInjectableValuesException
}

private[finatra] object InjectableValuesException {
  def apply(
    parentClass: Class[_],
    fieldName: String
  ): JacksonInjectableValuesException = new JacksonInjectableValuesException(
    s"Injection of @${classOf[InjectableValue].getSimpleName}-annotated case class fields is not " +
      s"supported for $fieldName in class $parentClass due to an invalid configuration. Please " +
      "check that your object mapper is properly configured to inject " +
      s"@${classOf[InjectableValue].getSimpleName}-annotated case class fields as " +
      "Jackson InjectableValues."
  )

  def apply(
    parentClass: Class[_],
    fieldName: String,
    key: Key[_],
    cause: Throwable
  ): JacksonInjectableValuesException =
    new JacksonInjectableValuesException(
      s"Unable to inject field '$fieldName' with $key into class $parentClass",
      cause)

  def apply(message: String): JacksonInjectableValuesException =
    new JacksonInjectableValuesException(message)
}
