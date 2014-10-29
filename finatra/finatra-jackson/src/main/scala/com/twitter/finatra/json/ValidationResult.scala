package com.twitter.finatra.json

object ValidationResult {

  def apply(valid: Boolean, reason: => String): ValidationResult = {
    new ValidationResult(valid, reason)
  }

  def valid: ValidationResult = valid("")

  def valid(reason: String) = ValidationResult(valid = true, reason)

  def invalid(reason: String) = ValidationResult(valid = false, reason)
}

// NOTE: Since "case classes" cannot have "by name" params, we need to use a
// normal class and then generate our own equals/hashcode
class ValidationResult private(
  val valid: Boolean,
  _reason: => String) {

  lazy val reason = _reason

  /* Generated */

  def canEqual(other: Any): Boolean = other.isInstanceOf[ValidationResult]

  override def equals(other: Any): Boolean = other match {
    case that: ValidationResult =>
      (that canEqual this) &&
        reason == that.reason &&
        valid == that.valid
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(reason, valid)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}
