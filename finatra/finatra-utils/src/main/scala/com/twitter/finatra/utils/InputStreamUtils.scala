package com.twitter.finatra.utils

import java.io.{BufferedInputStream, BufferedReader, InputStream, Reader}

object InputStreamUtils {

  def makeMarkable(is: InputStream): InputStream = {
    if (is.markSupported())
      is
    else
      new BufferedInputStream(is)
  }

  def makeMarkable(reader: Reader): Reader = {
    if (reader.markSupported())
      reader
    else
      new BufferedReader(reader)
  }
}
