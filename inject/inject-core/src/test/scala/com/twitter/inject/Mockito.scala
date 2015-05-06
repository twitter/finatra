package com.twitter.inject

import org.mockito.Matchers
import org.specs2.matcher.ScalaTestExpectations

/**
 * Provides Specs2 Mockito syntax sugar for ScalaTest.
 *
 * This is a drop-in replacement for org.specs2.mock.Mockito. Don't use
 * org.specs2.mock.Mockito directly. Otherwise, match failures won't be
 * propagated up as ScalaTest test failures.
 */
trait Mockito
  extends org.specs2.mock.Mockito
  with ScalaTestExpectations
  with Logging {

  protected def meq[T](obj: T): T = {
    Matchers.eq(obj)
  }

  protected def eqManifest[T: Manifest]: Manifest[T] = {
    meq(manifest[T])
  }

  protected def reset(mocks: AnyRef*) {
    for (mock <- mocks) {
      trace("Resetting " + mock)
      org.mockito.Mockito.reset(mock)
    }
  }
}
