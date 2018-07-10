package com.twitter.inject.tests.conversions

import com.twitter.inject.Test
import com.twitter.inject.conversions.bytearray._

import java.nio.charset.StandardCharsets

class ByteArrayConversionsTest extends Test {

  test("verify that the same bytes are logged the same") {
    val bytes = "DEADBEEF".getBytes(StandardCharsets.UTF_8)
    val loggable = bytes.toLoggable
    val bytes2 = ("DEAD" + "BEEF").getBytes(StandardCharsets.UTF_8)
    val loggable2 = bytes2.toLoggable
    assert(loggable == loggable2)
  }

  test("verify that different bytes are logged differently") {
    val bytes = "DEADBEEF".getBytes(StandardCharsets.UTF_8)
    val loggable = bytes.toLoggable

    val bytes2 = "DIFFERENTBEEF".getBytes(StandardCharsets.UTF_8)
    val loggable2 = bytes2.toLoggable
    assert(loggable2 != loggable)
  }

  test("test with a known quantity") {
    val bytes = Array(0x1, 0x2, 0x3, 0x4).map(_.toByte)
    val actual = bytes.toLoggable
    val expected = "Length: 4, Sha1: 12dada1fff4d4787ade3333147202c3b443e376f, Base64: AQIDBA=="
    assert(actual == expected)
  }
}
