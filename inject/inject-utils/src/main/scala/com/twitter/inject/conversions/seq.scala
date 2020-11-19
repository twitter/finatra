package com.twitter.inject.conversions

import com.twitter.conversions.SeqOps

@deprecated("Use com.twitter.conversions.SeqOps instead", "2020-11-16")
object seq {

  implicit class RichSeq[A](val self: Seq[A]) extends AnyVal {

    @deprecated("Use com.twitter.conversions.SeqOps#createMap instead", "2020-11-16")
    def createMap[K, V](keys: A => K, values: A => V): Map[K, V] =
      SeqOps.createMap(self, keys, values)

    @deprecated("Use com.twitter.conversions.SeqOps#createMap instead", "2020-11-16")
    def createMap[K, V](values: A => V): Map[A, V] = SeqOps.createMap(self, values)

    @deprecated("Use com.twitter.conversions.SeqOps#foreachPartial instead", "2020-11-16")
    def foreachPartial(pf: PartialFunction[A, Unit]): Unit = SeqOps.foreachPartial(self, pf)

    /**
     * Chooses last element in seq when key collision occurs
     */
    @deprecated("Use com.twitter.conversions.SeqOps#groupBySingleValue instead", "2020-11-16")
    def groupBySingleValue[B](keys: A => B): Map[B, A] = {
      createMap(keys, identity)
    }

    @deprecated("Use com.twitter.conversions.SeqOps#findItemAfter instead", "2020-11-16")
    def findItemAfter(itemToFind: A): Option[A] = SeqOps.findItemAfter(self, itemToFind)
  }
}
