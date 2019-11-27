package com.twitter.finatra.validation.tests

import com.twitter.finatra.validation.NotEmpty

case class CaseClassWithTwoConstructors(id: Long, @NotEmpty name: String) {
  def this(id: Long) = this(id, "New User")
}
