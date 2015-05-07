package com.twitter.inject.thrift.internal

import java.lang.reflect.Method

import com.google.inject.internal.asm.$Type
import com.google.inject.internal.cglib.core.$Signature
import com.google.inject.internal.cglib.proxy.$MethodProxy
import com.twitter.util.{Future, Memoize}
import org.aopalliance.intercept.MethodInvocation

/*
 * Note: invocation.proceed() does not work correctly when called multiple times from different threads.
 * As such, we create a thread safe "proceedDirectly" which directly calls the intercepted method skipping any additional interceptors.
 *
 * See the following for more details: https://groups.google.com/forum/#!topic/google-guice/vtyEEKgivdA
 */
object ThreadsafeFutureMethodInvocation {

  /* Public */

  def proceedDirectly(invocation: MethodInvocation): Future[Any] = {
    methodProxy(invocation.getMethod, invocation.getThis.getClass).invokeSuper(
      invocation.getThis,
      invocation.getArguments).asInstanceOf[Future[Any]]
  }

  /* Private */

  private val methodProxy = Memoize { methodAndClass: (Method, Class[_]) =>
    val (method, clazz) = methodAndClass

    $MethodProxy.find(
      clazz,
      new $Signature(
        method.getName,
        $Type.getReturnType(method),
        $Type.getArgumentTypes(method)))
  }
}
