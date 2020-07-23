package com.twitter.inject

import com.google.inject.name.Names

/**
 * A convenience [[TwitterModule]] intended to make it easy to bind a
 * mock instance to the object graph for testing. Users should instead
 * prefer the [[http://twitter.github.io/finatra/user-guide/testing/bind_dsl.html bind[T] DSL]]
 * instead.
 */
@deprecated(
  "Users should prefer the bind[T] DSL such that the lifecycle of a mock can be properly " +
    "managed within a given test. See: http://twitter.github.io/finatra/user-guide/testing/bind_dsl.html",
  "2020-07-22")
abstract class TwitterTestModule extends TwitterModule with Mockito {

  protected def bindToMock[T: Manifest]: T = {
    val mocked = smartMock[T]
    bind[T].toInstance(mocked)
    mocked
  }

  protected def bindToMock[T: Manifest](name: String): T = {
    val mocked = smartMock[T]
    bind[T].annotatedWith(Names.named(name)).toInstance(mocked)
    mocked
  }

  protected def bindToMock[T: Manifest, Ann <: java.lang.annotation.Annotation: Manifest]: T = {
    val mocked = smartMock[T]
    val annotation = manifest[Ann].runtimeClass.asInstanceOf[Class[Ann]]
    bind[T].annotatedWith(annotation).toInstance(mocked)
    mocked
  }
}
