package com.twitter.finatra.json.internal.caseclass.validation.domain

import com.twitter.finatra.json.annotations.Min
import com.twitter.finatra.json.internal.CarMake
import com.twitter.finatra.json.internal.caseclass.validation.{MethodValidation, ValidationResult}

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
