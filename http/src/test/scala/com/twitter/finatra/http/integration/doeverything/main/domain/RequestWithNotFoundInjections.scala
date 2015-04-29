package com.twitter.finatra.http.integration.doeverything.main.domain

import com.twitter.finatra.request.RequestInject

case class RequestWithNotFoundInjections(
   @RequestInject fooClass: FooClass)
