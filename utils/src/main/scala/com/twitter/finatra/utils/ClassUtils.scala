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
   * Returns `true` if it is NOT considered a case class.
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
  private[twitter] def notCaseClass(cls: Class[_]): Boolean = {
    (!PRODUCT.isAssignableFrom(cls)) ||
    OPTION.isAssignableFrom(cls) ||
    LIST.isAssignableFrom(cls) ||
    cls.getName.startsWith("scala.Tuple") ||
    cls.getName.startsWith("scala.util.Either")
  }

  /**
   * This is the negation of [[ClassUtils.notCaseClass]] It returns
   * `true` if the given class type is considered a case class.
   *
   * @param cls runtime representation of class type.
   */
  private[twitter] def isCaseClass(cls: Class[_]): Boolean = !notCaseClass(cls)
}
