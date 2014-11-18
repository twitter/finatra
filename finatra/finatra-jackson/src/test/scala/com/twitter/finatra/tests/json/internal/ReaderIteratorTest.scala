package com.twitter.finatra.tests.json.internal

import com.twitter.finatra.json.internal.streaming.ReaderIterator
import com.twitter.finatra.utils.Logging
import java.io.{InputStreamReader, Reader}
import org.apache.commons.io.IOUtils
import org.junit.runner.RunWith
import org.scalatest.WordSpec
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers

@RunWith(classOf[JUnitRunner])
class ReaderIteratorTest extends WordSpec with ShouldMatchers with Logging {

  "newline delimited strings" in {
    val fileReader = createReader("/newline_delimited_keyed_ids.json")
    val lineReaders = ReaderIterator.newlineDelimited(fileReader)
    val lines = (lineReaders map IOUtils.toString).toSeq
    lines.size should be(3)
    lines(0) should include("cars1")
    lines(1) should include("cars2")
    lines(2) should include("cars3")
  }

  private def createReader(filename: String): Reader = {
    val is = getClass.getResourceAsStream(filename)
    new InputStreamReader(is, "UTF-8")
  }
}
