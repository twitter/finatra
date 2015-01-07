package com.twitter.finatra.serialization


trait JsonSerializer {
  def serialize[T](item: T): Array[Byte]
}

