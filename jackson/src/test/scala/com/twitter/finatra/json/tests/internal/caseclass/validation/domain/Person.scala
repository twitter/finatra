package com.twitter.finatra.json.tests.internal.caseclass.validation.domain

import com.twitter.finatra.validation.NotEmpty
import org.joda.time.DateTime

case class Person(
   @NotEmpty name: String,
   nickname: String = "unknown",
   dob: Option[DateTime] = None,
   address: Option[Address] = None)
