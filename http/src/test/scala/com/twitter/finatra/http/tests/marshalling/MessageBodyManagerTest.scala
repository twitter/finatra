package com.twitter.finatra.http.tests.marshalling

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.internal.marshalling.MessageBodyManager
import com.twitter.finatra.http.marshalling.MessageBodyReader
import com.twitter.finatra.http.modules.{MessageBodyModule, MustacheModule}
import com.twitter.finatra.json.modules.FinatraJacksonModule
import com.twitter.inject.Test
import com.twitter.inject.app.TestInjector
import org.specs2.mock.Mockito

class MessageBodyManagerTest extends Test with Mockito {

  val request = mock[Request]
  val injector = TestInjector(MessageBodyModule, FinatraJacksonModule, MustacheModule)

  val messageBodyManager = injector.instance[MessageBodyManager]
  messageBodyManager.add[DogMessageBodyReader]()

  "parse car" in {
    messageBodyManager.add[Car2MessageBodyReader]()
  }

  "parse dog" in {
    messageBodyManager.read[Dog](request) should equal(Dog("Dog"))
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
