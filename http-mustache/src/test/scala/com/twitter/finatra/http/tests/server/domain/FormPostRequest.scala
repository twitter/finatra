package com.twitter.finatra.http.tests.server.domain

import com.twitter.finatra.http.annotations.FormParam

case class FormPostRequest(@FormParam name: String, @FormParam age: Int)
