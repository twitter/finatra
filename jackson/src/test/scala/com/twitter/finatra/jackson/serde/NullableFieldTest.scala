package com.twitter.finatra.jackson.serde

import com.twitter.finatra.jackson.caseclass.exceptions.CaseClassMappingException
import com.twitter.finatra.jackson.{CaseClassWithEnum, NullableField, NullableFieldDefault, ScalaObjectMapper}
import com.twitter.inject.Test

class NullableFieldTest extends Test {
  private val mapper: ScalaObjectMapper = ScalaObjectMapper()

  def parse[T: Manifest](string: String): T = {
    mapper.parse[T](string)
  }

  test("Allow null values") {
    parse[NullableField]("""{"value":null}""") should be(NullableField(null))
  }

  test("Fail on missing value even when null allowed") {
    val e = intercept[CaseClassMappingException] {
      parse[NullableField]("""{}""")
    }
    e.errors.map { _.getMessage } should equal(Seq("value: field is required"))
  }

  test("Allow null values when default is provided") {
    parse[NullableFieldDefault]("""{"value":null}""") should be(NullableFieldDefault(null))
  }

  test("Use default when null is allowed, but not provided") {
    parse[NullableFieldDefault]("""{}""") should be(NullableFieldDefault("foo"))
  }
}
