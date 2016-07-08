package com.twitter.finatra.conversions


object boolean {

  implicit class RichBoolean(val self: Boolean) extends AnyVal {

    def option[A](func: => A): Option[A] = {
      if (self)
        Some(func)
      else
        None
    }

    def onTrue(func: => Unit): Boolean = {
      if (self) {
        func
      }
      self
    }

    def onFalse(func: => Unit): Boolean = {
      if (!self) {
        func
      }
      self
    }
  }

}
