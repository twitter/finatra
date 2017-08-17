package com.twitter.inject

import com.google.inject.name.Names

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
