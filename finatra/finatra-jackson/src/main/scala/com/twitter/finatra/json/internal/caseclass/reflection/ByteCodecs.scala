package com.twitter.finatra.json.internal.caseclass.reflection

object ByteCodecs {

  val decodeMethod = {
    val cl = getClass.getClassLoader

    val byteCodecsClass = try {
      cl.loadClass("scala.reflect.generic.ByteCodecs") // 2.9.x
    } catch {
      case e: ClassNotFoundException =>
        cl.loadClass("scala.reflect.internal.pickling.ByteCodecs") // 2.10.x
    }

    byteCodecsClass.getMethod("decode")
  }

  /*
   * Bridge to support 2.9.x and 2.10.x
   */
  def decode(xs: Array[Byte]): Int = {
    decodeMethod.invoke(xs).asInstanceOf[Int]
  }
}
