package com.twitter.finatra.utils

import com.twitter.inject.TypeUtils
import com.twitter.util.Memoize
import scala.reflect.runtime.universe._

object ClassUtils {

  private val PRODUCT: Type = typeOf[Product]
  private val OPTION: Type = typeOf[Option[_]]
  private val LIST: Type = typeOf[List[_]]

  /**
   * Safely compute the `clazz.getSimpleName` of a given [[Class]] handling malformed
   * names with a best-effort to parse the final segment after the last `.` after
   * transforming all `$` to `.`.
   */
  val simpleName: Class[_] => String = Memoize { clazz: Class[_] =>
    // class.getSimpleName can fail with an InternalError (for malformed class/package names),
    // so we attempt to deal with this with a manual translation if necessary
    try {
      clazz.getSimpleName
    } catch {
      case _: InternalError =>
        // replace all $ with . and return the last element
        clazz.getName.replace('$', '.').split('.').last
    }
  }

  /**
   * This is the negation of [[ClassUtils.isCaseClass]]
   * Determine if a given class type is a case class.
   * Returns `true` if it is NOT considered a case class.
   *
   * @param clazz runtime representation of class type.
   */
  private[twitter] def notCaseClass[T](clazz: Class[T]): Boolean = !isCaseClass(clazz)

  /**
   * Returns  `true` if the given class type is considered a case class.
   * True if:
   *  - is assignable from PRODUCT and
   *  - not assignable from OPTION nor LIST and
   *  - is not a Tuple and
   *  - class symbol is case class.
   *
   * @param clazz runtime representation of class type.
   */
  private[twitter] def isCaseClass[T](clazz: Class[T]): Boolean = {
    val tpe = TypeUtils.asTypeTag(clazz).tpe
    val classSymbol = tpe.typeSymbol.asClass
    tpe <:< PRODUCT &&
    !(tpe <:< OPTION || tpe <:< LIST) &&
    !clazz.getName.startsWith("scala.Tuple") &&
    classSymbol.isCaseClass
  }
}
