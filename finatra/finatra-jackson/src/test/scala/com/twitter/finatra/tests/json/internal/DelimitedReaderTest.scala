package com.twitter.finatra.tests.json.internal

import com.twitter.finatra.json.internal.streaming.DelimitedReader
import java.io.StringReader
import org.apache.commons.io.IOUtils
import org.scalatest.WordSpec
import org.scalatest.matchers.ShouldMatchers

class DelimitedReaderTest extends WordSpec with ShouldMatchers {

  "no newline" in {
    val str = "abcdefg"
    val splittingReader = newlineSplittingReader(str)

    IOUtils.toString(splittingReader) should equal(str)
  }

  "one newline" in {
    val str = "abcdefg"
    val splittingReader = newlineSplittingReader(str + "\n")

    IOUtils.toString(splittingReader) should equal(str)
  }

  "multiple newlines" in {
    val str1 = "abcdefg"
    val str2 = "hijkl"
    val str3 = "mnop"
    val splittingReader = newlineSplittingReader("%s\n%s\n%s\n".format(str1, str2, str3))

    IOUtils.toString(splittingReader) should equal(str1)
    splittingReader.isDone should be(false)
    IOUtils.toString(splittingReader) should equal(str2)
    splittingReader.isDone should be(false)
    IOUtils.toString(splittingReader) should equal(str3)
    splittingReader.isDone should be(true)
  }

  def newlineSplittingReader(str: String) = {
    val reader = new StringReader(str)
    new DelimitedReader(reader, '\n')
  }
}
