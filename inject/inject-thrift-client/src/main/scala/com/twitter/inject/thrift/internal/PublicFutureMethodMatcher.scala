package com.twitter.inject.thrift.internal

import com.google.inject.matcher.AbstractMatcher
import com.twitter.util.Future
import java.lang.reflect.{Method, Modifier}

object PublicFutureMethodMatcher extends AbstractMatcher[Method] {
  override def matches(method: Method) = {
    !method.isSynthetic &&
      Modifier.isPublic(method.getModifiers) &&
      method.getReturnType == classOf[Future[_]]
  }
}
