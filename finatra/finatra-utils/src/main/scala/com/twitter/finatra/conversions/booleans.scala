package com.twitter.finatra.conversions


object booleans {

  class RichBoolean(boolean: Boolean) {

    def option[A](func: => A): Option[A] = {
      if (boolean)
        Some(func)
      else
        None
    }

    def onTrue(func: => Unit): Boolean = {
      if (boolean) {
        func
      }
      boolean
    }

    def onFalse(func: => Unit): Boolean = {
      if (!boolean) {
        func
      }
      boolean
    }

    def ifTrue(func: => String): String = {
      if (boolean)
        func
      else
        ""
    }

    def ifFalse(func: => String): String = {
      if (!boolean)
        func
      else
        ""
    }
  }

  implicit def booleanToRichBoolean(boolean: Boolean): RichBoolean = new RichBoolean(boolean)
}
