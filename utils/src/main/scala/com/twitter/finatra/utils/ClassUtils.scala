package com.twitter.finatra.utils

import com.twitter.util.reflect.Classes

@deprecated("Users should use c.t.util.reflect.Classes and c.t.util.reflect.Types", "2021-03-20")
object ClassUtils {

  /**
   * Safely compute the `clazz.getSimpleName` of a given [[Class]] handling malformed
   * names with a best-effort to parse the final segment after the last `.` after
   * transforming all `$` to `.`.
   */
  @deprecated("Users should use c.t.util.reflect.Classes#simpleName", "2021-03-20")
  val simpleName: Class[_] => String = { clazz: Class[_] =>
    Classes.simpleName(clazz)
  }
}
