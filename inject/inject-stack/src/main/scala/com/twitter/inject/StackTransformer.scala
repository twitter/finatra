package com.twitter.inject

import com.twitter.finagle.Stack

/**
 * StackTransformer allows plugins to modify Finatra filters before they are
 * applied to controller routes.
 *
 * Related to the Finagle [[com.twitter.finagle.StackTransformer]], which
 * modifies Finagle server stacks, the Finatra [[StackTransformer]] modifies
 * Finatra controller routes.
 *
 * If the goal is to share functionality between Finagle and Finatra, it's
 * reasonable to want to use Finagle's StackTransformer directly on Finatra
 * routes. However, the unit of functionality, and therefore meaningful reuse,
 * is the stack module - the StackTransformer merely adapts stack modules to
 * various stack contexts.
 *
 * The stack module as the unit of reuse also makes it possible to install them
 * in specific contexts; for example, modules that should be installed at the
 * service-level but not at the route-level (e.g. admission controllers).
 */
abstract class StackTransformer extends Stack.Transformer {
  def name: String

  override def toString: String = s"StackTransformer(name=$name)"
}
