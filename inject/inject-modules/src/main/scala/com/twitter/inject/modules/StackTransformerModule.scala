package com.twitter.inject.modules

import com.twitter.finagle.{ServiceFactory, Stack, StackTransformer}
import com.twitter.finagle.util.LoadService
import com.twitter.inject.TwitterModule

/**
 * Provides a [[com.twitter.finagle.StackTransformer]] to the dependency
 * injection context.
 */
object StackTransformerModule extends TwitterModule {
  override protected def configure(): Unit =
    bindSingleton[StackTransformer].toInstance(loadStackTransformer)

  private[this] def loadStackTransformer: StackTransformer =
    new StackTransformer {
      private[this] val transformers = LoadService[StackTransformer]

      val name: String = StackTransformerModule.this.getClass.getName

      def apply[Req, Rep](stackSvcFac: Stack[ServiceFactory[Req, Rep]]): Stack[ServiceFactory[Req, Rep]] =
        transformers.foldLeft(stackSvcFac)((stack, transform) => transform(stack))
    }
}
