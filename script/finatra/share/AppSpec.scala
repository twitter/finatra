package ###PACKAGE_NAME###

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

class AppSpec extends FlatSpec with ShouldMatchers {
  "An App" should "pass" in {
    (1) should equal(1)
  }
}
