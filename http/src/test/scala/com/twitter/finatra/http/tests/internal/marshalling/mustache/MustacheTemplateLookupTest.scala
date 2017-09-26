package com.twitter.finatra.http.tests.internal.marshalling.mustache

import com.twitter.finatra.http.internal.marshalling.mustache.{MustacheTemplate, MustacheTemplateLookup}
import com.twitter.finatra.http.marshalling.mustache.MustacheBodyComponent
import com.twitter.finatra.response.Mustache
import com.twitter.inject.Test

class MustacheTemplateLookupTest extends Test {

  final val htmlContentType = "text/html; charset=utf-8"
  final val jsonContentType = "application/json; charset=utf-8"

  @Mustache("testhtml")
  case class HtmlView(name: String)

  val htmlView = HtmlView("HTMel")
  val htmlComponent = MustacheBodyComponent(
    htmlView,
    "testotherhtml.mustache",
    htmlContentType
  )

  @Mustache(
    value = "testjson",
    contentType = jsonContentType
  )
  case class JSONView(name: String)

  val jsonView = JSONView("JSONDay")
  val jsonComponent = MustacheBodyComponent(
    jsonView,
    "testotherjson.mustache",
    jsonContentType
  )

  case class NoAnnotation()
  val noAnnotationView = NoAnnotation
  val noAnnotationComponent = MustacheBodyComponent(noAnnotationView, "", jsonContentType)

  val lookup = new MustacheTemplateLookup()

  /*
      Test default HTML views
   */
  test(
    "MustacheTemplateNameLookup#getTemplate returns the correct mustache file when supplied an annotated class"
  ) {
    lookup.getTemplate(htmlView) should be(
      MustacheTemplate(
        contentType = htmlContentType,
        name = "testhtml.mustache"
      ))
  }
  test(
    "MustacheTemplateNameLookup#getTemplate returns the correct mustache file when supplied a mustache body component with a different template"
  ) {
    lookup.getTemplate(htmlComponent) should be(
      MustacheTemplate(
        contentType = htmlContentType,
        name = "testotherhtml.mustache"
      )
    )
  }
  test(
    "MustacheTemplateNameLookup#getTemplate returns the data object's @Mustache template name when supplied a mustache body component without a template name"
  ) {
    lookup.getTemplate(htmlComponent.copy(templateName = "")) should be(
      MustacheTemplate(
        contentType = htmlContentType,
        name = "testhtml.mustache"
      )
    )
  }

  /*
    Test JSON views
   */
  test(
    "MustacheTemplateNameLookup#getTemplate returns the correct mustache file when supplied an annotated class with JSON content type"
  ) {
    lookup.getTemplate(jsonView) should be(
      MustacheTemplate(
        contentType = jsonContentType,
        name = "testjson.mustache"
      ))
  }
  test(
    "MustacheTemplateNameLookup#getTemplate returns the correct JSON mustache file when supplied a mustache body component with a different template"
  ) {
    lookup.getTemplate(jsonComponent) should be(
      MustacheTemplate(
        contentType = jsonContentType,
        name = "testotherjson.mustache"
      )
    )
  }

  /*
    Test when no template name is given and/or the data object has no @Mustache annotation
   */
  test(
    "MustacheTemplateLookup#getTemplate throws IllegalArgumentException when looking up template information for object without @Mustache"
  ) {
    intercept[IllegalArgumentException] {
      lookup.getTemplate(noAnnotationView)
    }.getMessage should be(s"Object ${noAnnotationView.getClass.getCanonicalName} has no Mustache annotation")
  }
  test(
    "MustacheTemplateLookup#getTemplate throws IllegalArgumentException when looking up template information for component with empty template name and no @Mustache"
  ) {
    intercept[IllegalArgumentException] {
      lookup.getTemplate(noAnnotationComponent)
    }.getMessage should be(s"Object ${noAnnotationView.getClass.getCanonicalName} has no Mustache annotation")
  }
}