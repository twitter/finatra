package com.twitter.finatra.example.main.domain

import com.google.common.net.MediaType
import com.twitter.finatra.conversions.json._
import com.twitter.finatra.marshalling.{MessageBodyWriter, WriterResponse}

class CarMessageBodyWriter extends MessageBodyWriter[Car] {

  override def write(car: Car) = {
    WriterResponse(
      MediaType.JSON_UTF_8,
      Map("car" -> car.name).toJsonBytes)
  }
}

case class Car(name: String)