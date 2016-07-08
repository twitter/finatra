package com.twitter.finatra.http.tests.integration.doeverything.main.domain

import com.fasterxml.jackson.databind.JsonNode
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.marshalling.MessageBodyReader
import com.twitter.finatra.json.FinatraObjectMapper
import javax.inject.Inject

class DomainTestUserReader @Inject()(
  mapper: FinatraObjectMapper)
  extends MessageBodyReader[DomainTestUser] {

  override def parse(request: Request): DomainTestUser = {
    val jsonNode = mapper.parse[JsonNode](request)
    val testUser = mapper.parse[TestUser](jsonNode)
    DomainTestUser(testUser.name)
  }
}

