package com.twitter.inject

import com.twitter.util.{Duration, Future, Return, Throw}
import org.scalatest.concurrent.PatienceConfiguration.{Interval, Timeout}
import org.scalatest.concurrent.ScalaFutures.{FutureConcept, PatienceConfig}
import org.scalatest.time.{Millis, Seconds, Span}
import scala.language.implicitConversions

/**
 * ScalaTest provides a whenReady function that handles async computations. whenReady takes
 * a FutureConcept and an implicit PatienceConfig as parameters. Extend this trait in your
 * test to get an implicit conversion from a Twitter Future to a FutureConcept. It also provides
 * a default PatienceConfig which you can override in your test to suit your needs.
 *
 * @see <a href="http://doc.scalatest.org/3.0.0/index.html#org.scalatest.concurrent.ScalaFutures">ScalaFutures</a>
 */
trait WhenReadyMixin {
  protected implicit val patienceConfig =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(20, Millis))

  final protected implicit def convertTwitterFuture[T](twitterFuture: Future[T]): FutureConcept[T] =
    new FutureConcept[T] {
      def eitherValue: Option[Either[Throwable, T]] =
        twitterFuture.poll.map {
          case Return(result) => Right(result)
          case Throw(e) => Left(e)
        }
      // As per the ScalaTest docs if the underlying future does not support these they
      // must return false.
      def isExpired: Boolean = false
      def isCanceled: Boolean = false
    }

  final implicit def twitterDurationToScalaTestTimeout(duration: Duration): Timeout = {
    Timeout(Span(duration.inMillis, Millis))
  }

  final implicit def twitterDurationToScalaTestInterval(duration: Duration): Interval = {
    Interval(Span(duration.inMillis, Millis))
  }

}
