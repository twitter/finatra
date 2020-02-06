package com.twitter.finatra.json.tests

import com.twitter.finatra.jackson.tests.AbstractScalaObjectMapperTest
import com.twitter.finatra.json.FinatraObjectMapper

@deprecated("See the ScalaObjectMapperTest", "2019-12-10")
class FinatraObjectMapperTest extends AbstractScalaObjectMapperTest {
  /* Class under test */
  override protected val mapper: FinatraObjectMapper = FinatraObjectMapper.create()
}
