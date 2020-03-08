package com.twitter.finatra.http.tests.integration.doeverything.main.domain

import com.twitter.finatra.http.annotations.{FormParam, RouteParam}
  
case class FormPostRequest(@FormParam name: String, @FormParam age: Int, @RouteParam cardId: String)
