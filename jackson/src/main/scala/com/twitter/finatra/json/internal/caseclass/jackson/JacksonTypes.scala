package com.twitter.finatra.json.internal.caseclass.jackson

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.`type`.{ArrayType, TypeFactory}
import com.twitter.finatra.json.internal.caseclass.reflection._

private[json] object JacksonTypes {

  /* Public */

  /* Match order matters since Maps can be treated as collections of tuples */
  def javaType(typeFactory: TypeFactory, scalaType: ScalaType): JavaType = {
    if (scalaType.typeArguments.isEmpty || scalaType.isEnum)
      typeFactory.constructType(scalaType.runtimeClass)

    else if (scalaType.isMap)
      typeFactory.constructMapLikeType(
        scalaType.runtimeClass,
        javaType(typeFactory, scalaType.typeArguments(0)),
        javaType(typeFactory, scalaType.typeArguments(1)))

    else if (scalaType.isCollection)
      typeFactory.constructCollectionLikeType(
        scalaType.runtimeClass,
        javaType(typeFactory, scalaType.typeArguments.head))

    else if (scalaType.isArray)
      ArrayType.construct(
        primitiveAwareJavaType(typeFactory, scalaType.typeArguments.head),
        null, null)

    else
      typeFactory.constructParametrizedType(
        scalaType.runtimeClass,
        scalaType.runtimeClass,
        javaTypes(typeFactory, scalaType.typeArguments): _*)
  }

  /* Private */

  private def javaTypes(typeFactory: TypeFactory, scalaTypes: Seq[ScalaType]): Seq[JavaType] = {
    for (scalaType <- scalaTypes) yield {
      javaType(typeFactory, scalaType)
    }
  }

  private def primitiveAwareJavaType(typeFactory: TypeFactory, scalaType: ScalaType): JavaType = {
    if (scalaType.isPrimitive)
      typeFactory.constructType(scalaType.primitiveAwareErasure)
    else
      javaType(typeFactory, scalaType)
  }
}
