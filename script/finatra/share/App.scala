package ###PACKAGE_NAME###

import com.twitter.finatra._

object App {

  __EXAMPLEAPP__

  def main(args: Array[String]) = {
    FinatraServer.register(app)
    FinatraServer.start()
  }
}
