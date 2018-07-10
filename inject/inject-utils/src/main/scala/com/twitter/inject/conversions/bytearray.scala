package com.twitter.inject.conversions

import java.security.MessageDigest
import java.util.Base64
import javax.xml.bind.DatatypeConverter

object bytearray {

  private val base64Encoding = Base64.getEncoder

  implicit class RichByteArray[T](val self: Array[Byte]) extends AnyVal {
    def toLoggable: String = {
      val base64 = base64Encoding.encodeToString(self)
      val messageDigest = MessageDigest.getInstance("SHA-1")
      val digestBytes = messageDigest.digest(self)
      val hash = DatatypeConverter.printHexBinary(digestBytes).toLowerCase
      s"Length: ${self.length}, Sha1: $hash, Base64: $base64"
    }
  }
}
