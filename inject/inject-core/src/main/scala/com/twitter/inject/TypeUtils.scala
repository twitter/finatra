package com.twitter.inject

import com.google.inject.TypeLiteral
import com.google.inject.internal.MoreTypes.ParameterizedTypeImpl
import java.lang.reflect.Type


object TypeUtils {

  def singleTypeParam[T](objType: Type): Type = objType match {
    case parametricType: ParameterizedTypeImpl =>
      parametricType.getActualTypeArguments.head
  }

  def superTypeFromClass(clazz: Class[_], superClazz: Class[_]): Type = {
    TypeLiteral.get(clazz).getSupertype(superClazz).getType
  }
}
