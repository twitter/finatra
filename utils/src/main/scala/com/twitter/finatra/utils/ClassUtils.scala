package com.twitter.finatra.utils

import com.twitter.util.Memoize

object ClassUtils {

  val simpleName = Memoize { clazz: Class[_] =>
    clazz.getSimpleName
  }
}
