package com.twitter.finatra.http.tests.server.domain

import com.twitter.finatra.request.FormParam

case class FormPostRequest(@FormParam name: String, @FormParam age: Int)
