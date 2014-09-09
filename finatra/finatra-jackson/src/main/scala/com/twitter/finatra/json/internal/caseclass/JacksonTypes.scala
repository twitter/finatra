package com.twitter.finatra.json.internal.caseclass

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.`type`.{ArrayType, CollectionLikeType, MapLikeType, TypeFactory}
import com.twitter.finatra.json.internal.caseclass.reflection._

object JacksonTypes {

  /* Public */

  /* Match order matters since Maps can be treated as collections of tuples */
  def javaType(typeFactory: TypeFactory, scalaType: ScalaType): JavaType = {
    if (scalaType.typeArguments.isEmpty || scalaType.isEnum)
      typeFactory.constructType(scalaType.erasure)

    else if (scalaType.isMap)
      MapLikeType.construct(
        scalaType.erasure,
        javaType(typeFactory, scalaType.typeArguments(0)),
        javaType(typeFactory, scalaType.typeArguments(1)))

    else if (scalaType.isCollection)
      CollectionLikeType.construct(
        scalaType.erasure,
        javaType(typeFactory, scalaType.typeArguments.head))

    else if (scalaType.isArray)
      ArrayType.construct(
        primitiveAwareJavaType(typeFactory, scalaType.typeArguments.head),
        null, null)

    else
      typeFactory.constructParametricType(
        scalaType.erasure,
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