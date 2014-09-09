package com.twitter.finatra.test

import com.twitter.finatra.json.FinatraObjectMapper

trait TestMapper {

  protected val mapper = FinatraObjectMapper.create()
}
