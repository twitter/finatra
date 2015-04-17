package com.twitter.finatra.integration.doeverything.main.domain

import com.twitter.finatra.request._

case class FormPostRequest(
   @FormParam name: String,
   @FormParam age: Int)
