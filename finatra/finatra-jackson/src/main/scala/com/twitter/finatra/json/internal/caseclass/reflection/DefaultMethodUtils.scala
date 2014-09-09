package com.twitter.finatra.json.internal.caseclass.reflection

object DefaultMethodUtils {

  def defaultFunction(clazz: Class[_], idx: Int): Option[() => Object] = {
    val argNum = idx + 1
    clazz.getMethods.find { method =>
      method.getName == "init$default$" + argNum || // Scala 2.9.x
        method.getName == "$lessinit$greater$default$" + argNum // Scala 2.10.x
    } map { method =>
      () => method.invoke(null)
    }
  }
}
