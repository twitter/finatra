package com.twitter.finatra.guice

import com.google.inject.name.Names
import org.specs2.mock.Mockito

abstract class GuiceTestModule
  extends GuiceModule
  with Mockito {

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

  protected def bindToMock[T: Manifest, Ann <: java.lang.annotation.Annotation : Manifest]: T = {
    val mocked = smartMock[T]
    val annotation = manifest[Ann].erasure.asInstanceOf[Class[Ann]]
    bind[T].annotatedWith(annotation).toInstance(mocked)
    mocked
  }
}