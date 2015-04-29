package com.twitter.finatra.utils

import org.jboss.netty.handler.codec.http.HttpResponse

object ResponseUtils {

  def is5xxResponse(response: HttpResponse) = {
    errorClass(response) == 5
  }

  def is4xxOr5xxResponse(response: HttpResponse) = {
    val errClass = errorClass(response)
    errClass == 4 || errClass == 5
  }

  private def errorClass(response: HttpResponse): Int = {
    response.getStatus.getCode / 100
  }
}
