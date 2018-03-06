package com.twitter.inject.conversions

import com.twitter.inject.utils.StringUtils
import org.apache.commons.lang.{StringUtils => ApacheCommonsStringUtils}

object string {

  implicit class RichString(val self: String) extends AnyVal {
    def toOption = {
      if (self == null || self.isEmpty)
        None
      else
        Some(self)
    }

    def getOrElse(default : => String) = {
      if (self == null || self.isEmpty)
        default
      else
        self
    }

    def ellipse(len: Int) = {
      ApacheCommonsStringUtils.abbreviate(self, len + 3) // adding 3 for the ellipses :-/
    }

    /**
     * Converts foo_bar to fooBar (first letter lowercased)
     *
     * For example:
     *
     * {{{
     *   "hello_world".camelify
     * }}}
     *
     * will return the String `"helloWorld"`.
     */
    def camelify = {
      StringUtils.camelify(self)
    }

    /**
     * Converts foo_bar to FooBar (first letter uppercased)
     *
     * For example:
     *
     * {{{
     *   "hello_world".pascalify
     * }}}
     *
     * will return the String `"HelloWorld"`.
     */
    def pascalify = {
      StringUtils.pascalify(self)
    }

    /**
     * Converts FooBar to foo_bar (all lowercase)
     *
     *  For example:
     *
     * {{{
     *   "HelloWorld".snakify
     * }}}
     *
     * will return the String `"hello_world"`.
     */
    def snakify = {
      StringUtils.snakify(self)
    }
  }
}
