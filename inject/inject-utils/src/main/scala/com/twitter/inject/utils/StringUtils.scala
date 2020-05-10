package com.twitter.inject.utils

import com.twitter.conversions.StringOps

object StringUtils {

  /**
   * Turn a string of format "FooBar" into snake case "foo_bar"
   *
   * @note snakify is not reversible, ie. in general the following will _not_ be true:
   *
   * {{{
   *    s == camelify(snakify(s))
   * }}}
   *
   * @return the underscored string
   */
  @deprecated(
    "Users are encouraged to use com.twitter.conversions.StringOps#toSnakeCase",
    "2019-03-02")
  def snakify(name: String): String = StringOps.toSnakeCase(name)

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
   * @param name the String to PascalCase
   * @return the PascalCased string
   */
  @deprecated(
    "Users are encouraged to use com.twitter.conversions.StringOps#toPascalCase",
    "2019-03-02")
  def pascalify(name: String): String = StringOps.toPascalCase(name)

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
   * @param name the String to camelCase
   * @return the camelCased string
   */
  @deprecated(
    "Users are encouraged to use com.twitter.conversions.StringOps#toCamelCase",
    "2019-03-02")
  def camelify(name: String): String = StringOps.toCamelCase(name)
}
