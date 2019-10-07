package com.twitter.finatra.json.internal.caseclass.utils

import org.json4s.reflect.ClassDescriptor

private[json] object DefaultMethodUtils {

  def defaultFunction(
    clazzDescriptor: ClassDescriptor,
    constructorParamIdx: Int
  ): Option[() => Object] = {
    val defaultMethodArgNum = constructorParamIdx + 1
    for {
      singletonDescriptor <- clazzDescriptor.companion
      companionObjectClass = singletonDescriptor.erasure.erasure
      companionObject = singletonDescriptor.instance
      method <- companionObjectClass.getMethods.find(_.getName == "$lessinit$greater$default$" + defaultMethodArgNum)
    } yield {
      () => method.invoke(companionObject)
    }
  }
}
