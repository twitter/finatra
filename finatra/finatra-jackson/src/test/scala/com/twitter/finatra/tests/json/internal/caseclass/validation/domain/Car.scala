package com.twitter.finatra.tests.json.internal.caseclass.validation.domain

import com.twitter.finatra.tests.json.internal.CarMake
import com.twitter.finatra.validation.{CommonMethodValidations, MethodValidation, Min, ValidationResult}
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
  warrantyEnd: Option[DateTime]) {

  @MethodValidation
  def validateId = {
    ValidationResult(
      id % 2 == 1,
      "id may not be even")
  }

  @MethodValidation
  def ownershipTimesValid = {
    CommonMethodValidations.validateTimeRange(
      ownershipStart,
      ownershipEnd,
      "ownershipStart",
      "ownershipEnd")
  }

  @MethodValidation
  def warrantyTimeValid = {
    CommonMethodValidations.validateTimeRange(
      warrantyStart,
      warrantyEnd,
      "warrantyStart",
      "warrantyEnd")
  }
}
