package com.twitter.finatra.json.internal.caseclass.reflection

private[json] object DefaultMethodUtils {

  def defaultFunction(companionObjectClass: Class[_], companionObject: Any, constructorParamIdx: Int): Option[() => Object] = {
    val defaultMethodArgNum = constructorParamIdx + 1
    companionObjectClass.getMethods.find { method =>
      method.getName == "$lessinit$greater$default$" + defaultMethodArgNum
    } map { method =>
      () => method.invoke(companionObject)
    }
  }
}
