package ###PACKAGE_NAME###

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import com.twitter.finatra.test._
import ###PACKAGE_NAME###._

class AppSpec extends SpecHelper {

  val app = new App.ExampleApp

  ###EXAMPLESPEC###

}
