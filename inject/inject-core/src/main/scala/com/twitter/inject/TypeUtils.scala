package com.twitter.inject

import com.google.inject.internal.MoreTypes.ParameterizedTypeImpl
import com.google.inject.util.Types
import com.google.inject.{Key, TypeLiteral}
import com.twitter.util.Future
import java.lang.reflect.Type
import scala.reflect.{ClassTag, _}


object TypeUtils {
  def singleTypeParam[T](objType: Type) = {
    objType match {
      case parametricType: ParameterizedTypeImpl =>
        parametricType.getActualTypeArguments.head
    }
  }

  /*
   * Workaround for not being able to use Higher Kinded Types with Manifests.
   * e.g. Passing MyThriftService[Future] into a type param of T:Manifest
   * will give a scalac error of "erroneous or inaccessible type"
   */
  def createKeyWithFutureTypeParam[T: ClassTag]: Key[T] = {
    val ct = classTag[T]

    val thriftClientType = Types.newParameterizedType(
      ct.runtimeClass,
      classOf[Future[_]])

    val typeLiteral = TypeLiteral.get(thriftClientType)


    Key.get(typeLiteral).asInstanceOf[Key[T]]
  }
}
