package com.twitter.inject.tests

import com.twitter.app.FlagFactory
import com.twitter.inject.WordSpecTest

class FlagFactoryTest extends WordSpecTest {

  "FlagFactory" should {

    // creates but does not register a flag
    "create a flag with no default" in {
      val flag = FlagFactory.create[String](
        name = "foo.bar.flag",
        help = "This is a test flag")
      flag.isDefined should be(false)
      flag.get shouldBe None
    }

    "create a flag with default" in {
      def defaultValue(): String = "production"

      // creates but does not register a flag
      val flag = FlagFactory.create[String](
        name = "bar.baz.flag",
        default = defaultValue(),
        help = "This is another test flag")
      flag.isDefined should be(false)
      flag.get shouldBe None
    }
  }
}
