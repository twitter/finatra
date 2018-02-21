package com.twitter.inject.utils

object StringUtils {

  private[this] val SnakifyRegexFirstPass = """([A-Z]+)([A-Z][a-z])""".r
  private[this] val SnakifyRegexSecondPass = """([a-z\d])([A-Z])""".r

  /**
   * Turn a string of format "FooBar" into snake case "foo_bar"
   *
   * @note snakify is not reversible, ie. in general the following will _not_ be true:
   *
   * {{{
   *    s == camelify(snakify(s))
   * }}}
   *
   * Copied from the Lift Framework:
   * https://github.com/lift/framework/blob/master/core/util/src/main/scala/net/liftweb/util/StringHelpers.scala
   *
   * Apache 2.0 License: https://github.com/lift/framework/blob/master/LICENSE.txt
   *
   * @return the underscored string
   */
  def snakify(name: String): String =
    SnakifyRegexSecondPass
      .replaceAllIn(
        SnakifyRegexFirstPass
          .replaceAllIn(name, "$1_$2"),
        "$1_$2"
      )
      .toLowerCase

  /**
   * Turns a string of format "foo_bar" into PascalCase "FooBar"
   *
   * @note Camel case may start with a capital letter (called "Pascal Case" or "Upper Camel Case") or,
   *       especially often in programming languages, with a lowercase letter. In this code's
   *       perspective, PascalCase means the first char is capitalized while camelCase means the first
   *       char is lowercased. In general both can be considered equivalent although by definition
   *       "CamelCase" is a valid camel-cased word. Hence, PascalCase can be considered to be a
   *       subset of camelCase.
   *
   * Copied from the "lift" framework:
   * https://github.com/lift/framework/blob/master/core/util/src/main/scala/net/liftweb/util/StringHelpers.scala
   *
   * Apache 2.0 License: https://github.com/lift/framework/blob/master/LICENSE.txt
   *
   * Functional code courtesy of Jamie Webb (j@jmawebb.cjb.net) 2006/11/28
   *
   * @param name the String to PascalCase
   * @return the PascalCased string
   */
  def pascalify(name: String): String = {
    def loop(x: List[Char]): List[Char] = (x: @unchecked) match {
      case '_' :: '_' :: rest => loop('_' :: rest)
      case '_' :: c :: rest => Character.toUpperCase(c) :: loop(rest)
      case '_' :: Nil => Nil
      case c :: rest => c :: loop(rest)
      case Nil => Nil
    }

    if (name == null) {
      ""
    } else {
      loop('_' :: name.toList).mkString
    }
  }

  /**
   * Turn a string of format "foo_bar" into camelCase with the first letter in lower case: "fooBar"
   *
   * This function is especially used to camelCase method names.
   *
   * @note Camel case may start with a capital letter (called "Pascal Case" or "Upper Camel Case") or,
   *       especially often in programming languages, with a lowercase letter. In this code's
   *       perspective, PascalCase means the first char is capitalized while camelCase means the first
   *       char is lowercased. In general both can be considered equivalent although by definition
   *       "CamelCase" is a valid camel-cased word. Hence, PascalCase can be considered to be a
   *       subset of camelCase.
   *
   * Copied from the Lift Framework:
   * https://github.com/lift/framework/blob/master/core/util/src/main/scala/net/liftweb/util/StringHelpers.scala
   *
   * Apache 2.0 License: https://github.com/lift/framework/blob/master/LICENSE.txt
   *
   * @param name the String to camelCase
   * @return the camelCased string
   */
  def camelify(name: String): String = {
    val tmp: String = pascalify(name)
    if (tmp.length == 0) {
      ""
    } else {
      tmp.substring(0, 1).toLowerCase + tmp.substring(1)
    }
  }
}
