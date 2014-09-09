package com.twitter.finatra.json.internal.caseclass.validation

import com.twitter.finatra.json.annotations.NotEmpty

case class CaseClassWithTwoConstructors(id: Long, @NotEmpty name: String) {
  def this(id: Long) = this(id, "New User")
}
