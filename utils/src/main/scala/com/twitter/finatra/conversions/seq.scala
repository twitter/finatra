package com.twitter.finatra.conversions

import scala.annotation.tailrec
import scala.collection.breakOut

object seq {

  implicit class RichSeq[A](val self: Seq[A]) extends AnyVal {

    def createMap[K, V](
      keys: A => K,
      values: A => V): Map[K, V] = {

      self.map { elem =>
        keys(elem) -> values(elem)
      }(breakOut)
    }

    def createMap[K, V](
      values: A => V): Map[A, V] = {

      self.map { elem =>
        elem -> values(elem)
      }(breakOut)
    }

    def foreachPartial(
      pf: PartialFunction[A, Unit]): Unit = {

      self.foreach { elem =>
        if (pf.isDefinedAt(elem)) {
          pf(elem)
        }
      }
    }

    /**
     * Chooses last element in seq when key collision occurs
     */
    def groupBySingleValue[B](keys: A => B): Map[B, A] = {
      createMap(
        keys,
        identity)
    }

    def findItemAfter(itemToFind: A): Option[A] = {
      @tailrec
      def recurse(itemToFind: A, seq: Seq[A]): Option[A] = seq match {
        case Seq(x, xs@_*) if x == itemToFind => xs.headOption
        case Seq(x, xs@_*) => recurse(itemToFind, xs)
        case Seq() => None
      }
      recurse(itemToFind, self)
    }
  }
}
