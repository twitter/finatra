package com.twitter.finatra.json.internal.caseclass.reflection

import scala.tools.scalap.scalax.rules.scalasig.TypeRefType

case class ConstructorParam(
  name: String,
  typeRefType: TypeRefType) {

  val scalaType = ScalaType(typeRefType)
}
