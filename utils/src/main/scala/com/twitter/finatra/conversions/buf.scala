package com.twitter.finatra.conversions

import com.twitter.io.Buf

object buf {

  implicit class RichBuf(val self: Buf) extends AnyVal {

    def utf8str: String = {
      Buf.Utf8.unapply(self).getOrElse(throw new Exception("Cannot create utf8 string"))
    }
  }

}
