package com.twitter.finatra.http.tests.internal.marshalling.mustache

import com.twitter.finatra.http.internal.marshalling.mustache.MustacheTemplateNameLookup
import com.twitter.finatra.http.marshalling.mustache.MustacheBodyComponent
import com.twitter.finatra.response.Mustache
import com.twitter.inject.Test

class MustacheTemplateNameLookupTest extends Test {

  @Mustache("teststandard")
  case class StandardView(name: String)

  val standardView = StandardView("Bob")

  val componentWith = MustacheBodyComponent(standardView, "testother.mustache")

  val componentWithout = MustacheBodyComponent(standardView, "")

  val lookup = new MustacheTemplateNameLookup()

  "MustacheTemplateNameLookup" should {
    "provide the correct mustache file" when {
      "supplied an annotated class" in {
        lookup.getTemplateName(standardView) should be("teststandard.mustache")
      }
      "supplied a mustache body component with a different template" in {
        lookup.getTemplateName(componentWith) should be("testother.mustache")
      }
      "supplied a mustache body component without a different template" in {
        lookup.getTemplateName(componentWithout) should be("teststandard.mustache")
      }
    }
  }

}
