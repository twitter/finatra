package com.twitter.finatra.http.integration.doeverything.main.domain

import javax.inject.Inject

case class RequestWithNotFoundInjections(
   @Inject fooClass: FooClass)
