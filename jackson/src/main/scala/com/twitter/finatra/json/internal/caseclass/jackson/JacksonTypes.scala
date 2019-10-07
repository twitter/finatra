package com.twitter.finatra.json.internal.caseclass.jackson

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.`type`.{ArrayType, TypeBindings, TypeFactory}

private[json] object JacksonTypes {

  /* Public */

  /**
   * Convert a `org.json4s.reflect.ScalaType` to a `com.fasterxml.jackson.databind.JavaType`.
   * This is used when defining the structure of a case class used in parsing JSON.
   *
   * @note order matters since Maps can be treated as collections of tuples
   */
  def javaType(typeFactory: TypeFactory, scalaType: org.json4s.reflect.ScalaType): JavaType = {
    // for backwards compatibility we box primitive types
    val erasedClassType = primitiveToObject(scalaType.erasure)

    if (scalaType.typeArgs.isEmpty || scalaType.erasure.isEnum) {
      typeFactory.constructType(erasedClassType)
    } else if (scalaType.isCollection) {
      if (scalaType.isMap) {
        typeFactory.constructMapLikeType(
          erasedClassType,
          javaType(typeFactory, scalaType.typeArgs.head),
          javaType(typeFactory, scalaType.typeArgs(1))
        )
      } else if (scalaType.isArray) {
        // need to special-case array creation
        ArrayType.construct(
          javaType(typeFactory, scalaType.typeArgs.head),
          TypeBindings
            .create(
              classOf[java.util.ArrayList[_]], // we hardcode the type to `java.util.ArrayList` to properly support Array creation
              javaType(typeFactory, scalaType.typeArgs.head)
            )
        )
      } else {
        typeFactory.constructCollectionLikeType(
          erasedClassType,
          javaType(typeFactory, scalaType.typeArgs.head)
        )
      }
    } else {
      typeFactory.constructParametricType(
        erasedClassType,
        javaTypes(typeFactory, scalaType.typeArgs): _*
      )
    }
  }

  /**
   * Convert a `org.json4s.reflect.ScalaType` to a `com.fasterxml.jackson.databind.JavaType`
   * taking into account any parameterized types and type bindings. This is used when parsing
   * JSON into a type.
   *
   * @note Match order matters since Maps can be treated as collections of tuples.
   */
  def javaType(
    typeFactory: TypeFactory,
    scalaType: org.json4s.reflect.ScalaType,
    caseClassParameterizedTypeBindings: Seq[String],
    actualTypeBindings: Map[String, JavaType]
  ): JavaType = {
    if (scalaType.typeArgs.isEmpty || scalaType.erasure.isEnum) {
      actualTypeBindings(caseClassParameterizedTypeBindings.head) // use first type
    } else if (scalaType.isCollection) {
      if (scalaType.isMap) {
        typeFactory.constructMapLikeType(
          scalaType.erasure,
          actualTypeBindings(caseClassParameterizedTypeBindings.head),
          actualTypeBindings(caseClassParameterizedTypeBindings.last)
        )
      } else if (scalaType.isArray) {
        // need to special-case array creation
        ArrayType.construct(
          javaType(
            typeFactory,
            scalaType.typeArgs.head,
            caseClassParameterizedTypeBindings,
            actualTypeBindings),
          TypeBindings
            .create(
              classOf[java.util.ArrayList[_]], // we hardcode the type to `java.util.ArrayList` to properly support Array creation
              actualTypeBindings(caseClassParameterizedTypeBindings.head)
            )
        )
      } else {
        typeFactory.constructCollectionLikeType(
          scalaType.erasure,
          actualTypeBindings(caseClassParameterizedTypeBindings.head)
        )
      }
    } else {
      typeFactory.constructParametricType(
        scalaType.erasure,
        caseClassParameterizedTypeBindings.map(actualTypeBindings): _*
      )
    }
  }

  /* Private */

  private def javaTypes(
    typeFactory: TypeFactory,
    scalaTypes: Seq[org.json4s.reflect.ScalaType]
  ): Seq[JavaType] = {
    for (scalaType <- scalaTypes) yield {
      javaType(typeFactory, scalaType)
    }
  }

  private[this] def primitiveToObject(clazz: Class[_]): Class[_] = clazz.getName match {
    case "boolean" => classOf[Boolean]
    case "byte" => classOf[Byte]
    case "short" => classOf[Short]
    case "int" => classOf[Int]
    case "long" => classOf[Long]
    case "float" => classOf[Float]
    case "double" => classOf[Double]
    case "char" => classOf[Char]
    case _ => clazz
  }
}
