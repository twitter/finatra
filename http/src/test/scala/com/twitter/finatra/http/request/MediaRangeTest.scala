/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */

// Adapted from github:playframework/framework/src/play/src/test/scala/play/api/http/MediaRangeSpec.scala
// under Apache 2 License (https://github.com/playframework/playframework#license)

package com.twitter.finatra.request

import com.twitter.finatra.http.request.{MediaType, MediaRange}
import com.twitter.inject.Test

class MediaRangeTest extends Test {

  "A MediaRange" should {

    def parseSingleMediaRange(mediaRange: String): MediaRange = {
      val parsed = MediaRange.parseAndSort(mediaRange)
      parsed.length shouldEqual 1
      parsed.head
    }

    def parseInvalidMediaRange(mediaRange: String) = {
      MediaRange.parseAndSort(mediaRange)
    }

    "accept all media types" in {
      val mediaRange = parseSingleMediaRange("*/*")
      mediaRange.accepts("text/html") shouldEqual true
      mediaRange.accepts("application/json") shouldEqual true
      mediaRange.accepts("foo/bar") shouldEqual true
    }

    "accept a range of media types" in {
      val mediaRange = parseSingleMediaRange("text/*")
      mediaRange.accepts("text/html") shouldEqual true
      mediaRange.accepts("text/plain") shouldEqual true
      mediaRange.accepts("application/json") shouldEqual false
    }

    "accept a media type" in {
      val mediaRange = parseSingleMediaRange("text/html")
      mediaRange.accepts("text/html") shouldEqual true
      mediaRange.accepts("text/plain") shouldEqual false
      mediaRange.accepts("application/json") shouldEqual false
    }

    "allow anything in a quoted string" in {
      MediaRange.parseAndSort("""foo/bar, foo2/bar2; p="v,/\"\\vv"; p2=v2""") shouldEqual Seq(
        new MediaRange("foo", "bar", Nil, None, Nil),
        new MediaRange("foo2", "bar2", Seq("p" -> Some("""v,/"\vv"""), "p2" -> Some("v2")), None, Nil)
      )
    }

    "extract the qvalue from the parameters" in {
      parseSingleMediaRange("foo/bar;q=0.25") shouldEqual new MediaRange("foo", "bar", Nil, Some(0.25), Nil)
    }

    "differentiate between media type parameters and accept extensions" in {
      parseSingleMediaRange("foo/bar;p1;q=0.25;p2") shouldEqual
        new MediaRange("foo", "bar", Seq("p1" -> None), Some(0.25), Seq("p2" -> None))
    }

    "support non spec compliant everything media ranges" in {
      parseSingleMediaRange("*") shouldEqual new MediaRange("*", "*", Nil, None, Nil)
    }

    "maintain the original order of media ranges in the accept header" in {
      MediaRange.parseAndSort("foo1/bar1, foo3/bar3, foo2/bar2") should contain theSameElementsInOrderAs List(
        new MediaRange("foo1", "bar1", Nil, None, Nil),
        new MediaRange("foo3", "bar3", Nil, None, Nil),
        new MediaRange("foo2", "bar2", Nil, None, Nil)
      )
    }

    "order by q value" in {
      MediaRange.parseAndSort("foo1/bar1;q=0.25, foo3/bar3, foo2/bar2;q=0.5") should contain theSameElementsInOrderAs List(
        new MediaRange("foo3", "bar3", Nil, None, Nil),
        new MediaRange("foo2", "bar2", Nil, Some(0.5), Nil),
        new MediaRange("foo1", "bar1", Nil, Some(0.25), Nil)
      )
    }

    "order by specificity" in {
      MediaRange.parseAndSort("*/*, foo/*, foo/bar") should contain theSameElementsInOrderAs List(
        new MediaRange("foo", "bar", Nil, None, Nil),
        new MediaRange("foo", "*", Nil, None, Nil),
        new MediaRange("*", "*", Nil, None, Nil)
      )
    }

    "order by parameters" in {
      MediaRange.parseAndSort("foo/bar, foo/bar;p1=v1;p2=v2, foo/bar;p1=v1") should contain theSameElementsInOrderAs List(
        new MediaRange("foo", "bar", Seq("p1" -> Some("v1"), "p2" -> Some("v2")), None, Nil),
        new MediaRange("foo", "bar", Seq("p1" -> Some("v1")), None, Nil),
        new MediaRange("foo", "bar", Nil, None, Nil)
      )
    }

    "order by parameters (complex)" in {
      info(MediaRange.parseAndSort("foo/bar1;q=0.25, */*;q=0.25, foo/*;q=0.25, foo/bar2, foo/bar3;q=0.5, foo/*, foo/bar4") )
      MediaRange.parseAndSort("foo/bar1;q=0.25, */*;q=0.25, foo/*;q=0.25, foo/bar2, foo/bar3;q=0.5, foo/*, foo/bar4") should contain theSameElementsInOrderAs List(
        new MediaRange("foo", "bar2", Nil, None, Nil),
        new MediaRange("foo", "bar4", Nil, None, Nil),
        new MediaRange("foo", "*", Nil, None, Nil),
        new MediaRange("foo", "bar3", Nil, Some(0.5), Nil),
        new MediaRange("foo", "bar1", Nil, Some(0.25), Nil),
        new MediaRange("foo", "*", Nil, Some(0.25), Nil),
        new MediaRange("*", "*", Nil, Some(0.25), Nil)
      )
    }

    "be able to be convert back to a string" in {
      new MediaType("foo", "bar", Nil).toString shouldEqual "foo/bar"
      new MediaType("foo", "bar", Seq("p1" -> Some("v1"), "p2" -> Some(""" v\"v"""), "p3" -> None)).toString shouldEqual
        """foo/bar; p1=v1; p2=" v\\\"v"; p3"""
      new MediaRange("foo", "bar", Nil, None, Nil).toString shouldEqual "foo/bar"
      new MediaRange("foo", "bar", Nil, Some(0.25), Nil).toString shouldEqual "foo/bar; q=0.25"
      new MediaRange("foo", "bar", Seq("p1" -> Some("v1")), Some(0.25), Seq("p2" -> Some("v2"))).toString shouldEqual
        "foo/bar; p1=v1; q=0.25; p2=v2"
    }

    // Rich tests
    "gracefully handle empty parts" in {
      parseInvalidMediaRange("text/")
      parseInvalidMediaRange("text/;foo")
    }

    "gracefully handle invalid characters in tokens" in {
      for {
        c <- "\u0000\u007F (){}\\\"".toSeq
        format <- Seq(
          "fo%so/bar, text/plain;charset=utf-8",
          "foo/ba%sr, text/plain;charset=utf-8",
          "text/plain;pa%sram;charset=utf-8",
          "text/plain;param=va%slue;charset=utf-8"
        )
      } yield {
        // Use URL encoder so we can see which ctl character it's using
        def description = "Media type format: '" + format + "' Invalid character: " + c.toInt
        val parsed = MediaRange.parseAndSort(format.format(c))

        parsed should have length 1
        parsed.head shouldEqual
          new MediaRange("text", "plain", Seq("charset" -> Some("utf-8")), None, Nil)
      }
    }

    "gracefully handle invalid q values" in {
      parseSingleMediaRange("foo/bar;q=a") shouldEqual new MediaRange("foo", "bar", Nil, None, Nil)
      parseSingleMediaRange("foo/bar;q=1.01") shouldEqual new MediaRange("foo", "bar", Nil, None, Nil)
    }
  }
}