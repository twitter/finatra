package com.twitter.finatra.conversions

import com.twitter.finatra.conversions.booleans._
import com.twitter.finatra.test.Test

class BooleanConversionsTest extends Test {

  "RichBoolean" should {
    "#option when true" in {
      true.option {
        1
      } should equal(Some(1))
    }

    "#option when false" in {
      false.option {
        1
      } should equal(None)
    }

    "trigger positive onTrue" in {
      var triggered = false
      true.onTrue {triggered = true} should equal(true)
      triggered should equal(true)
    }

    "trigger negative onTrue" in {
      var triggered = false
      false.onTrue {triggered = true} should equal(false)
      triggered should equal(false)
    }

    "trigger positive onFalse" in {
      var triggered = false
      false.onFalse {triggered = true} should equal(false)
      triggered should equal(true)
    }

    "trigger negative onFalse" in {
      var triggered = false
      true.onFalse {triggered = true} should equal(true)
      triggered should equal(false)
    }

    "#ifTrue with string" in {
      true.ifTrue("abc") should equal("abc")
      false.ifTrue("abc") should equal("")
    }

    "#ifFalse with string" in {
      false.ifFalse("abc") should equal("abc")
      true.ifFalse("abc") should equal("")
    }
  }
}
