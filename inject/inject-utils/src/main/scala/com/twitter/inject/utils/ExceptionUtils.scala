package com.twitter.inject.utils

import org.apache.commons.lang.StringUtils._

object ExceptionUtils {

  def stripNewlines(e: Throwable): String = {
    stripNewlines(e.toString)
  }

  def stripNewlines(str: String): String = {
    replace(str, "\n\twith NoSources", "")
  }
}
