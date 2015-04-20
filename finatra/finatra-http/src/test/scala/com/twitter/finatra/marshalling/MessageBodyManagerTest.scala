package com.twitter.finatra.marshalling

import com.twitter.finagle.http.Request
import com.twitter.finatra.json.modules.FinatraJacksonModule
import com.twitter.finatra.modules.{MessageBodyModule, MustacheModule}
import com.twitter.inject.Test
import com.twitter.inject.app.TestInjector
import org.specs2.mock.Mockito

class MessageBodyManagerTest extends Test with Mockito {

  val request = mock[Request]
  val injector = TestInjector(new MessageBodyModule, FinatraJacksonModule, MustacheModule)
  val messageBodyManager = injector.instance[MessageBodyManager]

  "parse objects from request" in {
    messageBodyManager.add[Car2MessageBodyReader]()
    messageBodyManager.add[DogMessageBodyReader]()

    messageBodyManager.parse[Car2](request) should equal(Car2("Car"))
    messageBodyManager.parse[Dog](request) should equal(Dog("Dog"))
  }
}

case class Car2(name: String)

case class Dog(name: String)

class Car2MessageBodyReader extends MessageBodyReader[Car2] {
  def parse(request: Request): Car2 = Car2("Car")
}

class DogMessageBodyReader extends MessageBodyReader[Dog] {
  def parse(request: Request): Dog = Dog("Dog")
}
