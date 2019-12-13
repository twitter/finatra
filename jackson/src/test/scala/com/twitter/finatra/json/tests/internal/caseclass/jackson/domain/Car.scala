package com.twitter.finatra.json.tests.internal.caseclass.jackson.domain

import com.twitter.finatra.json.tests.internal.CarMake
import com.twitter.finatra.validation.Min
import com.twitter.finatra.validation.{CommonMethodValidations, MethodValidation, ValidationResult}
import org.joda.time.DateTime

case class Car(
  id: Long,
  make: CarMake,
  model: String,
  @Min(2000) year: Int,
  owners: Seq[Person],
  @Min(0) numDoors: Int,
  manual: Boolean,
  ownershipStart: DateTime,
  ownershipEnd: DateTime,
  warrantyStart: Option[DateTime],
  warrantyEnd: Option[DateTime]
) {

  @MethodValidation
  def validateId: ValidationResult = {
    ValidationResult.validate(id % 2 == 1, "id may not be even")
  }

  @MethodValidation
  def validateYearBeforeNow: ValidationResult = {
    val thisYear = new DateTime().getYear
    val yearMoreThanOneYearInFuture: Boolean =
      if (year > thisYear) { (year - thisYear) > 1 } else false
    ValidationResult.validateNot(
      yearMoreThanOneYearInFuture,
      "Model year can be at most one year newer."
    )
  }

  @MethodValidation(fields=Array("ownershipEnd"))
  def ownershipTimesValid: ValidationResult = {
    CommonMethodValidations.validateTimeRange(
      ownershipStart,
      ownershipEnd,
      "ownershipStart",
      "ownershipEnd"
    )
  }

  @MethodValidation(fields=Array("warrantyStart", "warrantyEnd"))
  def warrantyTimeValid: ValidationResult = {
    CommonMethodValidations.validateTimeRange(
      warrantyStart,
      warrantyEnd,
      "warrantyStart",
      "warrantyEnd"
    )
  }
}
