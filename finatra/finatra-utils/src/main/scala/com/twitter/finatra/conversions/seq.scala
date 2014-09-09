package com.twitter.finatra.conversions

import scala.annotation.tailrec
import scala.collection.breakOut

object seq {

  class RichSeq[A](seq: Seq[A]) {

    def createMap[K, V](
      keys: A => K,
      values: A => V): Map[K, V] = {

      (seq map { elem =>
        keys(elem) -> values(elem)
      })(breakOut)
    }

    def createMap[K, V](
      values: A => V): Map[A, V] = {

      (seq map { elem =>
        elem -> values(elem)
      })(breakOut)
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
      recurse(itemToFind, seq)
    }
  }

  implicit def seqToRichSeq[A](seq: Seq[A]): RichSeq[A] = new RichSeq[A](seq)
}
