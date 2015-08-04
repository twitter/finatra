package com.twitter.hello

import javax.inject.Inject

import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finagle.http.{Response, Request}
import com.twitter.finatra.http.exceptions.ExceptionMapper
import com.twitter.finatra.http.response.ResponseBuilder
import com.twitter.util.Future

class MissingPetMapper @Inject()(response: ResponseBuilder)
    extends ExceptionMapper[MissingPet] {

  override def toResponse(request: Request, exception: MissingPet): Response = {
    response.notFound("Your pet doesn't exist! :(")
  }
}