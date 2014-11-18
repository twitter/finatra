package com.twitter.finatra.tests.json.internal.caseclass.validation.domain

import com.twitter.finatra.json.annotations.NotEmpty
import org.joda.time.DateTime

case class Person(
   @NotEmpty name: String,
   nickname: String = "unknown",
   dob: Option[DateTime] = None,
   address: Option[Address] = None)
