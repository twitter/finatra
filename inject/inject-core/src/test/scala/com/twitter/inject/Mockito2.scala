package com.twitter.inject

import org.mockito.ArgumentMatchers
import org.specs2.matcher.ScalaTestExpectations

/**
 * Provides Specs2 Mockito 2 syntax sugar for ScalaTest.
 *
 * This is a drop-in replacement for org.specs2.mock.Mockito. Don't use
 * org.specs2.mock.Mockito directly. Otherwise, match failures won't be
 * propagated up as ScalaTest test failures.
 */
trait Mockito2
  extends Mockito {

  override protected def meq[T](obj: T): T = {
    ArgumentMatchers.eq(obj)
  }
}
