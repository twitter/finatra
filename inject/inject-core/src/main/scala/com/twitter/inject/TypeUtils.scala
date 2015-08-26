package com.twitter.inject

import com.google.inject.internal.MoreTypes.ParameterizedTypeImpl
import java.lang.reflect.Type


object TypeUtils {
  def singleTypeParam[T](objType: Type) = {
    objType match {
      case parametricType: ParameterizedTypeImpl =>
        parametricType.getActualTypeArguments.head
    }
  }
}
