package com.twitter.finatra.tests.conversions

import com.twitter.finatra.conversions.seq._
import com.twitter.inject.WordSpecTest


class SeqConversionsTest extends WordSpecTest {

  "RichSeq" should {
    "#extractMap" in {
      val map = Seq("a", "and") createMap(_.size, _.toUpperCase)
      map should equal(Map(1 -> "A", 3 -> "AND"))
    }
    "#groupBySingle chooses last element in seq when key collision occurs" in {
      val map = Seq("a", "and", "the") groupBySingleValue {_.size}
      map should equal(Map(3 -> "the", 1 -> "a"))
    }
    "#findItemAfter" in {
      Seq(1, 2, 3).findItemAfter(1) should equal(Some(2))
      Seq(1, 2, 3).findItemAfter(2) should equal(Some(3))
      Seq(1, 2, 3).findItemAfter(3) should equal(None)
      Seq(1, 2, 3).findItemAfter(4) should equal(None)
      Seq(1, 2, 3).findItemAfter(5) should equal(None)
      Seq[Int]().findItemAfter(5) should equal(None)

      Stream(1, 2, 3).findItemAfter(1) should equal(Some(2))
      Stream[Int]().findItemAfter(5) should equal(None)

      Vector(1, 2, 3).findItemAfter(1) should equal(Some(2))
      Vector[Int]().findItemAfter(5) should equal(None)
    }
    "#foreachPartial" in {
      var numStrings = 0
      Seq("a", 1) foreachPartial {
        case str: String =>
          numStrings += 1
      }
      numStrings should equal(1)
    }
  }
}
