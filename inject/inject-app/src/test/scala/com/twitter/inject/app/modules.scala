package com.twitter.inject.app

import com.twitter.inject.TwitterModule
import com.twitter.inject.TypeUtils.asManifest
import java.lang.annotation.Annotation
import scala.reflect.runtime.universe._

private[inject] class InjectionServiceModule[T : TypeTag](
  instance: T) extends TwitterModule {

  override def configure(): Unit = {
    bind(asManifest[T]).toInstance(instance)
  }
}

private[inject] class InjectionServiceWithAnnotationModule[T : TypeTag, A <: Annotation : TypeTag](
  instance: T) extends TwitterModule {

  override def configure(): Unit = {
    bind(asManifest[T], asManifest[A]).toInstance(instance)
  }
}
