package com.twitter.finatra.integration.doeverything.main.domain

import com.twitter.finatra.request._

case class RequestWithNotFoundInjections(
   @RequestInject fooClass: FooClass)
