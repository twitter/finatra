package com.twitter.finatra.conversions

import com.twitter.io.Buf

object buf {

  implicit class RichBuf(val buf: Buf) extends AnyVal {

    def utf8str: String = {
      Buf.Utf8.unapply(buf).getOrElse(throw new Exception("Cannot create utf8 string"))
    }
  }

}
