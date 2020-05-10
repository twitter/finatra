package com.twitter.finatra.kafka.test

import com.twitter.conversions.DurationOps._
import com.twitter.finagle.{Addr, Dtab, Name, NameTree, Path}
import com.twitter.finatra.kafka.{utils => KafkaUtils}
import com.twitter.inject.Test
import com.twitter.util.{Activity, Duration, TimeoutException, Var}
import com.twitter.finagle.naming.{DefaultInterpreter, NameInterpreter}

class BootstrapServerUtilsTest extends Test {

  override protected def afterEach(): Unit = {
    NameInterpreter.global = DefaultInterpreter
  }

  test("lookup success") {
    KafkaUtils.BootstrapServerUtils
      .lookupBootstrapServers("/$/inet/localhost/88", Duration.Top) should equal("127.0.0.1:88")
  }

  test("lookup with timeout") {
    val testingPath: String = "/s/kafka/cluster"

    // Bind the testing path to a pending address, so the name resolution will time out
    NameInterpreter.global = new NameInterpreter {
      override def bind(dtab: Dtab, path: Path): Activity[NameTree[Name.Bound]] = {
        if (path.equals(Path.read(testingPath))) {
          Activity.value(NameTree.Leaf(Name.Bound(Var.value(Addr.Pending), new Object())))
        } else {
          DefaultInterpreter.bind(dtab, path)
        }
      }
    }

    val ex = the[TimeoutException] thrownBy {
      KafkaUtils.BootstrapServerUtils.lookupBootstrapServers(testingPath, 10.milliseconds)
    }
    ex.getMessage should equal("10.milliseconds")
  }
}
