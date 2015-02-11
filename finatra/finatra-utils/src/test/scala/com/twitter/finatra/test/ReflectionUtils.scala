package com.twitter.finatra.test

import java.lang.reflect.Field

object ReflectionUtils {

  def getDeclaredFieldsRespectingInheritance(clazz: Class[_]): Array[Field] = {
    if (clazz == null) {
      Array()
    } else {
      clazz.getDeclaredFields ++ getDeclaredFieldsRespectingInheritance(clazz.getSuperclass)
    }
  }
}
