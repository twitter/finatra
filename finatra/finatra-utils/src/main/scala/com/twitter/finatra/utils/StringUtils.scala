package com.twitter.finatra.utils

object StringUtils {

  def simpleName(clazz: Class[_ <: Any]): String = {
    clazz.getSimpleName.replaceAll("\\$", "")
  }
}
