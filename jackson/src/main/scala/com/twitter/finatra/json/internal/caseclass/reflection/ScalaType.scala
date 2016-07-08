package com.twitter.finatra.json.internal.caseclass.reflection

import scala.tools.scalap.scalax.rules.scalasig.TypeRefType

private[json] case class ScalaType(
  typeRefType: TypeRefType) {

  private val path = typeRefType.symbol.path

  /* Public */

  val runtimeClass: Class[_] = {
    CaseClassSigParser.loadClass(path)
  }

  def primitiveAwareErasure: Class[_] = {
    primitiveAwareLoadClass(path)
  }

  val typeArguments: Seq[ScalaType] = {
    val typeArgs = typeRefType.typeArgs.asInstanceOf[Seq[TypeRefType]]
    typeArgs map ScalaType.apply
  }

  def isPrimitive: Boolean = {
    runtimeClass != classOf[AnyRef]
  }

  def isCollection: Boolean = {
    classOf[Iterable[Any]].isAssignableFrom(runtimeClass)
  }

  def isMap: Boolean = {
    classOf[Map[Any, Any]].isAssignableFrom(runtimeClass)
  }

  def isArray: Boolean = {
    path == "scala.Array"
  }

  def isEnum: Boolean = {
    runtimeClass.isEnum
  }

  /* Private */

  /* Needed to support Array creation (Derived from Jerkson) */
  private def primitiveAwareLoadClass(path: String): Class[_] = path match {
    case "scala.Boolean" => classOf[Boolean]
    case "scala.Byte" => classOf[Byte]
    case "scala.Char" => classOf[Char]
    case "scala.Double" => classOf[Double]
    case "scala.Float" => classOf[Float]
    case "scala.Int" => classOf[Int]
    case "scala.Long" => classOf[Long]
    case "scala.Short" => classOf[Short]
    case _ => CaseClassSigParser.loadClass(path)
  }
}
