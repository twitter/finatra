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

  test(
    "MustacheTemplateNameLookup#provide the correct mustache file when supplied an annotated class"
  ) {
    lookup.getTemplateName(standardView) should be("teststandard.mustache")
  }
  test(
    "MustacheTemplateNameLookup#provide the correct mustache file when supplied a mustache body component with a different template"
  ) {
    lookup.getTemplateName(componentWith) should be("testother.mustache")
  }
  test(
    "MustacheTemplateNameLookup#provide the correct mustache file when supplied a mustache body component without a different template"
  ) {
    lookup.getTemplateName(componentWithout) should be("teststandard.mustache")
  }

}
