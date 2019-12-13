package com.twitter.finatra.http.tests.integration.doeverything.main.domain

import com.fasterxml.jackson.databind.JsonNode
import com.twitter.finagle.http.Message
import com.twitter.finatra.http.marshalling.mapper._
import com.twitter.finatra.http.marshalling.MessageBodyReader
import com.twitter.finatra.json.FinatraObjectMapper
import javax.inject.Inject

class DomainTestUserReader @Inject()(mapper: FinatraObjectMapper)
    extends MessageBodyReader[DomainTestUser] {

  override def parse(message: Message): DomainTestUser = {
    val jsonNode = mapper.parseMessageBody[JsonNode](message)
    val testUser = mapper.parse[TestUser](jsonNode)
    DomainTestUser(testUser.name)
  }
}
