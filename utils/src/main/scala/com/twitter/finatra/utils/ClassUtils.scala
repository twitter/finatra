package com.twitter.finatra.utils

import com.twitter.util.Memoize

object ClassUtils {

  private val PRODUCT: Class[Product] = classOf[Product]
  private val OPTION: Class[Option[_]] = classOf[Option[_]]
  private val LIST: Class[List[_]] = classOf[List[_]]

  val simpleName: Class[_] => String = Memoize { clazz: Class[_] =>
    clazz.getSimpleName
  }

  /**
   * Determine if a given class type is a case class.
   *
   * A class is NOT considered as a case class if one of the following criteria is met:
   * 1. the class is NOT the same as or the super class of class type [[Product]].
   * 2. the class is the same as or the super class of class type [[Option[_]].
   * 3. the class is the same as or the super class of class type [[List]].
   * 4. the class starts with `scala.Tuple`.
   * 5. the class starts with `scala.util.Either`.
   *
   * @param cls runtime representation of class type.
   */
  def maybeIsCaseClass(cls: Class[_]): Boolean = {
    if (!PRODUCT.isAssignableFrom(cls)) false
    else if (OPTION.isAssignableFrom(cls)) false
    else if (LIST.isAssignableFrom(cls)) false
    else if (cls.getName.startsWith("scala.Tuple")) false
    else if (cls.getName.startsWith("scala.util.Either")) false
    else true
  }
}
