package ###PACKAGE_NAME###

import com.twitter.finatra._
import com.twitter.finatra.ContentType._

object App extends FinatraServer {

  __EXAMPLEAPP__

  register(new ExampleApp())
}
