package org.specs2.matcher

import org.scalatest.exceptions.TestFailedException
import org.specs2.mock.Mockito

/**
 * This trait ensures specs2 Mockito errors result in ScalaTest errors.
 *
 * Simply mixing in ThrownExpectations results in compile errors
 * regarding specs and ScalaTest. See ThrownExpectations including
 * scaladoc describing integration with Scalatest.
 *
 * TODO(AF-177): come up with a less hacky solution.
 */
trait ScalaTestExpectations extends Mockito {

  override def createExpectable[T](t: => T, alias: Option[String => String]): Expectable[T] = {
    new Expectable(() => t) {
      override def check[S >: T](r: MatchResult[S]): MatchResult[S] = {
        r match {
          case f@MatchFailure(ok, ko, _, _) =>
            throw new TestFailedException(f.message, f.exception, 0) // We throw a ScalaTest exception here
          case _ =>
            ()
        }
        r
      }

      override def applyMatcher[S >: T](m: => Matcher[S]): MatchResult[S] = super.applyMatcher(m)

      override val desc = alias

      override def map[S](f: T => S): Expectable[S] = createExpectable(f(value), desc)

      override def mapDescription(d: Option[String => String]): Expectable[T] = createExpectable(value, d)

      override def evaluateOnce = {
        val v = t
        createExpectable(t, desc)
      }
    }
  }
}
