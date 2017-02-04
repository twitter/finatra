package com.twitter.finatra.http.tests.marshalling

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.internal.marshalling.MessageBodyManager
import com.twitter.finatra.http.marshalling.MessageBodyReader
import com.twitter.finatra.http.modules.{MessageBodyModule, MustacheModule}
import com.twitter.finatra.json.modules.FinatraJacksonModule
import com.twitter.inject.WordSpecTest
import com.twitter.inject.app.TestInjector
import org.specs2.mock.Mockito

class MessageBodyManagerTest extends WordSpecTest with Mockito {

  val request = mock[Request]
  val injector = TestInjector(MessageBodyModule, FinatraJacksonModule, MustacheModule)

  val messageBodyManager = injector.instance[MessageBodyManager]
  messageBodyManager.add[DogMessageBodyReader]()
  messageBodyManager.add[FatherMessageBodyReader]()
  messageBodyManager.add(new Car2MessageBodyReader)

  "parse car2" in {
    messageBodyManager.read[Car2](request) should equal(Car2("Car"))
  }

  "parse dog" in {
    messageBodyManager.read[Dog](request) should equal(Dog("Dog"))
  }

  "parse son with father MBR" in {
    messageBodyManager.read[Son](request) should equal(Son("Son"))
  }

  "parse father impl with father MBR" in {
    messageBodyManager.read[Son](request) should equal(Son("Son"))
  }
}

case class Car2(name: String)

case class Dog(name: String)

trait Father

case class Son(name: String) extends Father

case class FatherImpl(name: String) extends Father

class Car2MessageBodyReader extends MessageBodyReader[Car2] {
  def parse[M: Manifest](request: Request): Car2 = Car2("Car")
}

class DogMessageBodyReader extends MessageBodyReader[Dog] {
  def parse[M: Manifest](request: Request): Dog = Dog("Dog")
}

class FatherMessageBodyReader extends MessageBodyReader[Father] {
  override def parse[M <: Father : Manifest](request: Request): Father = {
    if (manifest[M].runtimeClass == classOf[Son]) {
      Son("Son").asInstanceOf[Father]
    } else if (manifest[M].runtimeClass == classOf[FatherImpl]) {
      FatherImpl("Father").asInstanceOf[Father]
    } else {
      throw new RuntimeException("FAIL")
    }
  }
}
