package com.twitter.finatra.conversions


object boolean {

  implicit class RichBoolean(boolean: Boolean) {

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
  }

}
