package com.twitter.finatra.validation

import jakarta.validation.ValidationException

case class InvalidCaseClassException(clazz: Class[_]) extends ValidationException {

  override def getMessage: String = s"Class [$clazz] is not a valid case class."
}
