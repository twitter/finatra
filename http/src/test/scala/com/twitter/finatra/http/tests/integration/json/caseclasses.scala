package com.twitter.finatra.http.tests.integration.json

import com.twitter.finagle.http.Request
import com.twitter.finatra.request.{Header, QueryParam, RouteParam}
import com.twitter.finatra.validation.{MethodValidation, Size, ValidationResult}

case class PersonWithThingsRequest(
  id: Int,
  name: String,
  age: Option[Int],
  @Size(min = 1, max = 10) things: Map[String, Things])

case class Things(
  @Size(min = 1, max = 2) names: Seq[String])

case class GenericWithRequest[T](data: T, request: Request)

case class WithBooleanRequest(
  @RouteParam id: Int,
  @QueryParam completeOnly: Boolean = false
)

trait TestRequest {
  protected[this] val ValidFormats: Seq[String] = Seq("compact", "default", "detailed")

  protected[this] def validateFormat(formatValue: Option[String], formatKey: String): ValidationResult = {
    if (formatValue.isEmpty) {
      ValidationResult.Valid()
    } else {
      val actualFormat = formatValue.get
      val errorMsg = s"Bad parameter value: <$actualFormat>." +
        s" The only format values allowed for <$formatKey> are ${ValidFormats.mkString(",")}"
      ValidationResult.validate(ValidFormats.contains(actualFormat), errorMsg)
    }
  }

  protected[this] def validateListOfLongIds(commaSeparatedListOfIds: String): Boolean = {
    val actualIdsString = commaSeparatedListOfIds.trim()
    if (actualIdsString.isEmpty) {
      false
    } else {
      actualIdsString
        .split("\\,").map { anEntry =>
        anEntry.trim.nonEmpty && anEntry.matches("\\d+")
      }.forall(_ == true)
    }
  }

  protected[this] def validateListOfUsers(users: Option[String]): Boolean = {
    users.forall { names =>
      val namesTrimmed = names.trim()
      if (namesTrimmed.isEmpty) {
        false
      } else {
        namesTrimmed
          .split("\\,").map { anEntry =>
          anEntry.trim.nonEmpty
        }.forall(_ == true)
      }
    }
  }

  protected[this] def extractListOfLongIds(idsString: String): Seq[Long] = {
    val items = idsString.trim().split(",")
    items.map { anItem =>
      anItem.toLong
    }.toSeq
  }

  def listOfStrings(namesValue: Option[String]): Seq[String] = {
    {
      namesValue.map {
        _.split(",").toSeq
      }
    }.getOrElse(Seq[String]())
  }

  def createErrorMessage(paramName: String, badValue: String, errMsg: String): String = {
    s"Bad Value: '$badValue' for parameter '$paramName'. $errMsg"
  }

  def createErrorMessage(paramName: String, badValue: Option[String], errMsg: String): String = {
    createErrorMessage(paramName, badValue.getOrElse("None"), errMsg)
  }
}
case class UserLookupRequest(
  @QueryParam ids: Option[String] = None,
  @QueryParam names: Option[String] = None,
  @QueryParam format: Option[String] = None,
  @QueryParam("user.format") userFormat: Option[String] = None,
  @QueryParam("status.format") statusFormat: Option[String] = None,
  @Header("Accept") acceptHeader: Option[String] = None)
  extends TestRequest {

  def validationPassesForIds: Boolean = ids.forall(validateListOfLongIds)
  def validationPassesForNames: Boolean = validateListOfUsers(names)

  @MethodValidation
  def validateIds(): ValidationResult =
    ValidationResult.validate(
      validationPassesForIds,
      createErrorMessage("ids", ids, "Must be a comma separated list of decimal numbers.")
    )

  @MethodValidation
  def validateNames(): ValidationResult = {
    ValidationResult.validate(
      validationPassesForNames,
      createErrorMessage(
        "names",
        names,
        "Must be a comma separated list of names."
      )
    )
  }

  @MethodValidation
  def validateUserFormat(): ValidationResult =
    validateFormat(userFormat, "user.format")

  @MethodValidation
  def validateStatusFormat(): ValidationResult =
    validateFormat(statusFormat, "status.format")

  @MethodValidation
  def validateMinimalRequestParams: ValidationResult = {
    // in case one of the validations failed, don't add this error message
    val atLeastOneValidEntry =
      (!validationPassesForIds || !validationPassesForNames) ||
        listOfIds.nonEmpty || listOfNames.nonEmpty
    ValidationResult.validate(
      atLeastOneValidEntry,
      "At least one valid id or one valid name must be provided"
    )
  }

  private[this] val listOfIds: Seq[Long] = ids.fold(Seq.empty[Long])(extractListOfLongIds)
  private[this] val listOfNames: Seq[String] = listOfStrings(names)
}
