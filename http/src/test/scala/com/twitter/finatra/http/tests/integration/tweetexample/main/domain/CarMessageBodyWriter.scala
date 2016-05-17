package com.twitter.finatra.http.tests.integration.tweetexample.main.domain

import com.google.common.net.MediaType
import com.twitter.finatra.http.marshalling.{MessageBodyWriter, WriterResponse}
import com.twitter.finatra.json.FinatraObjectMapper
import javax.inject.Inject

class CarMessageBodyWriter @Inject()(
  mapper: FinatraObjectMapper)
  extends MessageBodyWriter[Car] {

  override def write(car: Car) = {
    WriterResponse(
      MediaType.JSON_UTF_8,
      mapper.writeValueAsBytes(Map(
        "car" -> car.name)))
  }
}

case class FooCar(name: String) extends Car

case class BarCar(name: String) extends Car

trait Car {
  def name: String
}
