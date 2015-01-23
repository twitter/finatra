// The trait below needs to be inside a specs2 package because Expectable's
// constructor is package-protected.
package org.specs2.matcher

import org.scalatest.exceptions.TestFailedException

/**
 * Convert match failures into ScalaTest TestFailedExceptions.
 * The Specs2 documentation tells us to extend ThrownExpectations:
 * ```
 * trait ScalaTestExpectations extends ThrownExpectations {
 *   override protected def checkFailure[T](m: =>MatchResult[T]) = {
 *     m match {
 *       case f @ MatchFailure(ok, ko, _, _, _) => throw new TestFailedException(f.message, f.exception, 0)
 *       case _ => ()
 *     }
 *     m
 *   }
 * }
 * ```
 * However, this causes a compile error when org.scalatest.Suite is mixed in; we avoid
 * this by directly extending Expectations.
 */
trait ScalaTestExpectations extends Expectations {
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
