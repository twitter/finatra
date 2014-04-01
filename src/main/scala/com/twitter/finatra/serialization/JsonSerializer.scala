package com.twitter.finatra.serialization


/**
 * 2014-03-20
 * @author Michael Rose <michael@fullcontact.com>
 */

trait JsonSerializer {
  def serialize[T](item: T): Array[Byte]
}





