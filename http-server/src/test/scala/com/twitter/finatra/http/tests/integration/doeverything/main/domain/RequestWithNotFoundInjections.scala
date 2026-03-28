package com.twitter.finatra.http.tests.integration.doeverything.main.domain

import jakarta.inject.Inject

case class RequestWithNotFoundInjections(@Inject fooClass: FooClass)
