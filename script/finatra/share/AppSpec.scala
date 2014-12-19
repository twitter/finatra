package ###PACKAGE_NAME###

import org.scalatest.FlatSpec
import org.scalatest.Matchers
import com.twitter.finatra.test._
import com.twitter.finatra.FinatraServer
import ###PACKAGE_NAME###._

class AppSpec extends FlatSpecHelper {

  val app = new App.ExampleApp
  override val server = new FinatraServer
  server.register(app)

  __EXAMPLESPEC__

}
