package com.twitter.finatra.validation

case class InvalidCaseClassException(clazz: Class[_])
    extends ValidationException(Seq.empty[ValidationResult.Invalid]) {

  override def getMessage: String = s"Class [$clazz] is not a valid case class."
}
