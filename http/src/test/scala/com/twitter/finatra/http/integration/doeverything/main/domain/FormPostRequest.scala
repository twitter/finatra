package com.twitter.finatra.http.integration.doeverything.main.domain

import com.twitter.finatra.request.FormParam

case class FormPostRequest(
   @FormParam name: String,
   @FormParam age: Int)
