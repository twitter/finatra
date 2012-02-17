import org.junit.Test
import com.codahale.simplespec.Spec

class RouteSpec extends Spec {

  class `a thing` {

    @Test def `thing` = {
      val str = "str"
      str.must(be(str))
    }

  }


}

