package com.twitter.finatra.http.modules

import com.google.inject.Module
import com.twitter.finatra.http.annotations.Mustache
import com.twitter.finatra.http.marshalling.{MessageBodyManager, MustacheBodyComponent, MustacheMessageBodyWriter}
import com.twitter.finatra.mustache.modules.MustacheFactoryModule
import com.twitter.inject.{Injector, TwitterModule}

/**
 * This [[TwitterModule]] ties together Mustache and HTTP by registering a
 * Mustache-specific [[com.twitter.finatra.http.marshalling.MessageBodyWriter]]
 * to the [[com.twitter.finatra.http.marshalling.MessageBodyManager]]. The
 * writer is keyed by both the [[Mustache]] annotation
 * and [[MustacheBodyComponent]] [[com.twitter.finatra.http.marshalling.MessageBodyComponent]] type.
 */
object MustacheModule extends TwitterModule {

  override def modules: Seq[Module] = Seq(MustacheFactoryModule)

  /*
   This module needs a bound MessageBodyManager but we explicitly do not add the
   `MessageBodyModule` to the list of `modules` above as it is overridable in
   the HttpServerTrait and we may cause a collision by adding the default here.
   */
  override def singletonStartup(injector: Injector): Unit = {
    debug("Configuring Mustache")
    val manager = injector.instance[MessageBodyManager]
    manager.addWriterByAnnotation[Mustache, MustacheMessageBodyWriter]()
    manager.addWriterByComponentType[MustacheBodyComponent, MustacheMessageBodyWriter]()
  }

}
