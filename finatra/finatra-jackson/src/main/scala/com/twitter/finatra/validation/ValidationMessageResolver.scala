package com.twitter.finatra.validation

import java.lang.annotation.Annotation
import java.util.Properties
import scala.io.Source

class ValidationMessageResolver {

  val validationProperties = load

  def resolve(clazz: Class[_ <: Annotation], values: Any*): String = {
    val unresolvedMessage = validationProperties.getProperty(clazz.getName)
    if(unresolvedMessage == null)
      "unable to resolve error message due to unknown validation annotation: " + clazz
    else
      unresolvedMessage.format(values: _*)
  }

  private def load: Properties = {
    val properties = new Properties()
    loadBaseProperties(properties)
    loadPropertiesFromClasspath(properties)
    properties
  }

  private def loadBaseProperties(properties: Properties) {
    properties.load(getClass.getResourceAsStream("/com/twitter/finatra/json/validation.properties"))
  }

  private def loadPropertiesFromClasspath(properties: Properties) {
    val validationPropertiesUrl = getClass.getResource("/validation.properties")
    if(validationPropertiesUrl != null) {
      properties.load(validationPropertiesUrl.openStream())
    }
  }
}