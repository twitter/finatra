package com.twitter.finatra.json.internal.streaming

import com.twitter.finatra.annotations.Experimental
import com.twitter.finatra.utils.InputStreamUtils.makeMarkable
import java.io.Reader
import org.apache.commons.lang.ArrayUtils

/**
 * Reader that in addition to returning -1 at end of data, will also return -1 once each delimiter is found.
 * After receiving a -1, if isDone() is false, subsequent reads will continue after the last found delimiter.
 * The delimiter is not returned in read data.
 *
 * NOTE: Splitting a Reader into multiple Readers by a delimiter is best
 * exposed through ReaderIterator (which internally uses this class)
 *
 * TODO: Handle escaped delimiters
 */
@Experimental
class DelimitedReader(
  _reader: Reader,
  delimiter: Char)
  extends Reader {

  private val reader = makeMarkable(_reader)

  /* Mutable State */
  private var delimiterFoundInPriorRead: Boolean = false
  private var endOfStream = false

  /* Public */

  override def read(chars: Array[Char], offset: Int, maxLength: Int): Int = {
    if (delimiterFoundInPriorRead) {
      delimiterFoundInPriorRead = false
      -1
    }
    else {
      reader.mark(maxLength)
      val numCharsRead = reader.read(chars, offset, maxLength)
      if (numCharsRead == -1) {
        endOfStream = true
        -1
      }
      else {
        skipPastDelimiter(chars, offset, numCharsRead)
      }
    }
  }

  override def close() = {
    if (delimiterFoundInPriorRead) {
      clearDelimiterFound()
    }
    else {
      endOfStream = true
      reader.close()
    }
  }

  def isDone = endOfStream

  def clearDelimiterFound() {
    delimiterFoundInPriorRead = false
  }

  /* Private */

  /**
   * Skip past delimiter if found and update the "numReadChars" as appropriate
   * @return Updated numReadChars (up to, but not including delimiter if found)
   */
  private def skipPastDelimiter(chars: Array[Char], off: Int, numReadChars: Int): Int = {
    val delimiterIdx = ArrayUtils.indexOf(chars, delimiter, off)
    if (delimiterIdx == -1) {
      setDoneIfNextReadEmpty()
      numReadChars
    }
    else {
      delimiterFoundInPriorRead = true

      val charsBeforeDelim = delimiterIdx - off
      reader.reset()
      reader.skip(charsBeforeDelim + 1) //skip past delimiter

      setDoneIfNextReadEmpty()
      charsBeforeDelim
    }
  }

  /*
   * This method is required so that isDone is true when no additional bytes can be read.
   * Otherwise, isDone will be false, when the next read returns -1, which then causes
   * ReaderIterator.hasNext to be true when it really should be false.
   */
  private def setDoneIfNextReadEmpty() {
    reader.mark(1)
    val nextChar = reader.read()
    reader.reset()
    endOfStream = nextChar == -1
  }
}
