package com.twitter.inject.conversions

import com.twitter.conversions.StringOps
import org.apache.commons.{lang => acl}

object string {

  implicit class RichString(val self: String) extends AnyVal {
    def toOption: Option[String] = {
      if (self == null || self.isEmpty)
        None
      else
        Some(self)
    }

    def getOrElse(default : => String): String = {
      if (self == null || self.isEmpty)
        default
      else
        self
    }

    def ellipse(len: Int): String = {
      acl.StringUtils.abbreviate(self, len + 3) // adding 3 for the ellipses :-/
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
    @deprecated("Users are encouraged to use com.twitter.conversions.StringOps#toCamelCase", "2019-03-02")
    def camelify: String = StringOps.toCamelCase(self)

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
    @deprecated("Users are encouraged to use com.twitter.conversions.StringOps#toPascalCase", "2019-03-02")
    def pascalify: String = StringOps.toPascalCase(self)

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
    @deprecated("Users are encouraged to use com.twitter.conversions.StringOps#toSnakeCase", "2019-03-02")
    def snakify: String = StringOps.toSnakeCase(self)
  }
}
