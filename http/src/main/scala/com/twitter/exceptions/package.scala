package com.twitter.finatra

package object exceptions {
  @deprecated("Use com.twitter.finatra.http.exceptions.DefaultExceptionMapper", "")
  type DefaultExceptionMapper = com.twitter.finatra.http.exceptions.DefaultExceptionMapper

  @deprecated("Use com.twitter.finatra.http.exceptions.ExceptionMapper", "")
  type ExceptionMapper[T <: Throwable] = com.twitter.finatra.http.exceptions.ExceptionMapper[T]

  @deprecated("Use com.twitter.finatra.http.exceptions.HttpResponseException", "")
  type HttpResponseException = com.twitter.finatra.http.exceptions.HttpResponseException

  @deprecated("Use com.twitter.finatra.http.exceptions.HttpException", "")
  type HttpException = com.twitter.finatra.http.exceptions.HttpException

  @deprecated("Use com.twitter.finatra.http.exceptions.HttpException", "")
  val HttpException = com.twitter.finatra.http.exceptions.HttpException

  @deprecated("Use com.twitter.finatra.http.exceptions.NotFoundException", "")
  type NotFoundException = com.twitter.finatra.http.exceptions.NotFoundException

  @deprecated("Use com.twitter.finatra.http.exceptions.NotFoundException", "")
  val NotFoundException = com.twitter.finatra.http.exceptions.NotFoundException

  @deprecated("Use com.twitter.finatra.http.exceptions.ConflictException", "")
  type ConflictException = com.twitter.finatra.http.exceptions.ConflictException

  @deprecated("Use com.twitter.finatra.http.exceptions.ConflictException", "")
  val ConflictException = com.twitter.finatra.http.exceptions.ConflictException

  @deprecated("Use com.twitter.finatra.http.exceptions.InternalServerErrorException", "")
  type InternalServerErrorException = com.twitter.finatra.http.exceptions.InternalServerErrorException

  @deprecated("Use com.twitter.finatra.http.exceptions.InternalServerErrorException", "")
  val InternalServerErrorException = com.twitter.finatra.http.exceptions.InternalServerErrorException

  @deprecated("Use com.twitter.finatra.http.exceptions.ServiceUnavailableException", "")
  type ServiceUnavailableException = com.twitter.finatra.http.exceptions.ServiceUnavailableException

  @deprecated("Use com.twitter.finatra.http.exceptions.ServiceUnavailableException", "")
  val ServiceUnavailableException = com.twitter.finatra.http.exceptions.ServiceUnavailableException

  @deprecated("Use com.twitter.finatra.http.exceptions.BadRequestException", "")
  type BadRequestException = com.twitter.finatra.http.exceptions.BadRequestException

  @deprecated("Use com.twitter.finatra.http.exceptions.BadRequestException", "")
  val BadRequestException = com.twitter.finatra.http.exceptions.BadRequestException

  @deprecated("Use com.twitter.finatra.http.exceptions.ForbiddenException", "")
  type ForbiddenException = com.twitter.finatra.http.exceptions.ForbiddenException

  @deprecated("Use com.twitter.finatra.http.exceptions.ForbiddenException", "")
  val ForbiddenException = com.twitter.finatra.http.exceptions.ForbiddenException

  @deprecated("Use com.twitter.finatra.http.exceptions.NotAcceptableException", "")
  type NotAcceptableException = com.twitter.finatra.http.exceptions.NotAcceptableException

  @deprecated("Use com.twitter.finatra.http.exceptions.NotAcceptableException", "")
  val NotAcceptableException = com.twitter.finatra.http.exceptions.NotAcceptableException
}
