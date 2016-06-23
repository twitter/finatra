package com.twitter.finatra.json.internal.caseclass.reflection

import scala.tools.scalap.scalax.rules.scalasig.TypeRefType

private[json] case class ConstructorParam(
  name: String,
  typeRefType: TypeRefType) {

  val scalaType = ScalaType(typeRefType)
}
