package com.twitter.finatra.utils

import com.twitter.util.Memoize

object ClassUtils {

  val simpleName: Class[_] => String = Memoize { clazz: Class[_] =>
    clazz.getSimpleName
  }
}
