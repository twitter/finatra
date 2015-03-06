package com.twitter.inject.requestscope

import com.google.inject.Provider
import com.twitter.finagle.http.Request
import com.twitter.inject.TwitterModule
import java.lang.annotation.Annotation

trait RequestContextBinding extends TwitterModule {

  override final val modules = Seq(RequestContextModule)

  protected def bindRequestContext[T: Manifest, Ann <: Annotation : Manifest](field: Request.Schema.Field[T]): Unit = {
    bind[T].annotatedWith[Ann].toProvider(recordValueProvider(field))
  }

  protected def bindRequestContext[T: Manifest](field: Request.Schema.Field[T]): Unit = {
    bind[T].toProvider(recordValueProvider(field))
  }

  private def recordValueProvider[T](field: Request.Schema.Field[T]): Provider[T] = {
    val recordProvider = getProvider[Request.Schema.Record]
    new Provider[T] {
      override def get: T = recordProvider.get.apply(field)
    }
  }
}
