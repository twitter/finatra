package com.twitter.inject.tests.conversions

import com.twitter.inject.Test
import com.twitter.inject.conversions.boolean._

class BooleanConversionsTest extends Test {

  test("RichBoolean#option when true") {
    true.option {
      1
    } should equal(Some(1))
  }

  test("RichBoolean#option when false") {
    false.option {
      1
    } should equal(None)
  }

  test("RichBoolean#trigger positive onTrue") {
    var triggered = false
    true.onTrue { triggered = true } should equal(true)
    triggered should equal(true)
  }

  test("RichBoolean#trigger negative onTrue") {
    var triggered = false
    false.onTrue { triggered = true } should equal(false)
    triggered should equal(false)
  }

  test("RichBoolean#trigger positive onFalse") {
    var triggered = false
    false.onFalse { triggered = true } should equal(false)
    triggered should equal(true)
  }

  test("RichBoolean#trigger negative onFalse") {
    var triggered = false
    true.onFalse { triggered = true } should equal(true)
    triggered should equal(false)
  }
}
