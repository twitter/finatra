package com.twitter.finatra.http.tests.integration.doeverything.main.domain

import com.twitter.finatra.http.annotations.QueryParam
import com.twitter.util.jackson.ScalaObjectMapper
import javax.inject.Inject

class UnserializableRecord(var recurse: UnserializableRecord)
