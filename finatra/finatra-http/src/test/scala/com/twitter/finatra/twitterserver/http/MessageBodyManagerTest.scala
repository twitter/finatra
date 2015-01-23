package com.twitter.finatra.twitterserver.http

import com.twitter.finagle.http.Request
import com.twitter.finatra.guice.FinatraTestInjector
import com.twitter.finatra.json.modules.FinatraJacksonModule
import com.twitter.finatra.marshalling.{MessageBodyManager, MessageBodyReader}
import com.twitter.finatra.test.{Mockito, Test}
import com.twitter.finatra.modules.{MessageBodyModule, MustacheModule}

class MessageBodyManagerTest extends Test with Mockito {

  val request = mock[Request]

  "parse objects from request" in {
    val manager = createManager()
    manager.add[CarMessageBodyReader]()
    manager.add[DogMessageBodyReader]()

    manager.parse[Car](request) should equal(Car("Car"))
    manager.parse[Dog](request) should equal(Dog("Dog"))
  }

  def createManager(): MessageBodyManager = {
    val injector = FinatraTestInjector(MessageBodyModule, FinatraJacksonModule, MustacheModule)
    injector.instance[MessageBodyManager]
  }
}

case class Car(name: String)

case class Dog(name: String)

class CarMessageBodyReader extends MessageBodyReader[Car] {
  def parse(request: Request): Car = Car("Car")
}

class DogMessageBodyReader extends MessageBodyReader[Dog] {
  def parse(request: Request): Dog = Dog("Dog")
}
