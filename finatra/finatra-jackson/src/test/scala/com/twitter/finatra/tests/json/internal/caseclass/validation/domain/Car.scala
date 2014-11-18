package com.twitter.finatra.tests.json.internal.caseclass.validation.domain

import com.twitter.finatra.json.ValidationResult
import com.twitter.finatra.json.annotations.{MethodValidation, Min}
import com.twitter.finatra.tests.json.internal.CarMake

case class Car(
  id: Long,
  make: CarMake,
  model: String,
  @Min(2000) year: Int,
  owners: Seq[Person],
  @Min(0) numDoors: Int,
  manual: Boolean) {

  @MethodValidation
  def validateId = {
    ValidationResult(
      id % 2 == 1,
      "id may not be even")
  }

}
