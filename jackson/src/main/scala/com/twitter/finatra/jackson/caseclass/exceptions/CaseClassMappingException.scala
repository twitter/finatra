package com.twitter.finatra.jackson.caseclass.exceptions

import com.fasterxml.jackson.databind.JsonMappingException

object CaseClassMappingException {

  /**
   * Create a new [[CaseClassMappingException]] with no field exceptions.
   * @return a new [[CaseClassMappingException]]
   */
  def apply(): CaseClassMappingException = new CaseClassMappingException()

  /**
   * Create a new [[CaseClassMappingException]] over the given field exceptions.
   * @param fieldMappingExceptions exceptions to enclose in returned [[CaseClassMappingException]]
   * @return a new [[CaseClassMappingException]] with the given field exceptions.
   */
  def apply(
    fieldMappingExceptions: Set[CaseClassFieldMappingException]
  ): CaseClassMappingException =
    new CaseClassMappingException(fieldMappingExceptions)
}

/**
 * A subclass of [[JsonMappingException]] used to signal fatal problems with mapping of JSON
 * content to a Scala case class.
 *
 * Per-field details (of type [[CaseClassFieldMappingException]]) are carried to provide the
 * ability to iterate over all exceptions causing the failure to construct the case class.
 *
 * This extends [[JsonMappingException]] such that this exception is properly handled
 * when deserializing into nested case-classes.
 *
 * @see [[CaseClassFieldMappingException]]
 * @see [[com.fasterxml.jackson.databind.JsonMappingException]]
 */
class CaseClassMappingException private (
  fieldMappingExceptions: Set[CaseClassFieldMappingException] =
    Set.empty[CaseClassFieldMappingException])
    extends JsonMappingException(null, "") {

  /**
   * The collection of [[CaseClassFieldMappingException]] instances which make up this
   * [[CaseClassMappingException]]. This collection is intended to be purposely exhaustive in that
   * is specifies all errors encountered in mapping JSON content to a Scala case class.
   */
  val errors: Seq[CaseClassFieldMappingException] =
    fieldMappingExceptions.toSeq.sortBy(_.getMessage)

  override def getMessage: String = {
    "\nErrors:\t\t" + errors.mkString(", ") + "\n\n"
  }
}
