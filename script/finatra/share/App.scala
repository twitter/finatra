package ###PACKAGE_NAME###

import com.twitter.finatra._

object App {

  ###EXAMPLEAPP###

  def main(args: Array[String]) = {
    FinatraServer.register(app)
    FinatraServer.start()
  }
}
