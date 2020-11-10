package com.twitter.finatra.validation

import java.lang.annotation.Annotation
import java.util.Properties
import scala.reflect.ClassTag

/**
 * To resolve error messages for the type of validation failure. May be pattern-matched
 * to customize handling of specific failures.
 *
 * Can be overridden to customize and localize error messages.
 */
class MessageResolver {

  private[validation] val validationProperties: Properties = load

  @deprecated("Use resolve[Ann <: Annotation](values: Any*)", "2020-10-09")
  def resolve(clazz: Class[_ <: Annotation], values: Any*): String = {
    val unresolvedMessage = validationProperties.getProperty(clazz.getName)
    if (unresolvedMessage == null)
      "unable to resolve error message due to unknown validation annotation: " + clazz
    else
      unresolvedMessage.format(values: _*)
  }

  /**
   * Resolve the passed object reference for a given Constraint [[Annotation]] type.
   * @param values object references to resolve using the reference Constraint [[Annotation]].
   * @param clazzTag implicit [[ClassTag]] for the given [[Annotation]] type param.
   * @tparam Ann the Constraint [[Annotation]] to use for message resolution.
   * @return resolved [[String]] from the inputs.
   */
  def resolve[Ann <: Annotation](values: Any*)(implicit clazzTag: ClassTag[Ann]): String = {
    // Note: the method signature is equivalent to `def resolve[Ann <: Annotation: ClassTag](..): String`
    val clazz = clazzTag.runtimeClass
    val unresolvedMessage = validationProperties.getProperty(clazz.getName)
    if (unresolvedMessage == null)
      "unable to resolve error message due to unknown validation annotation: " + clazz
    else
      unresolvedMessage.format(values: _*)
  }

  private[this] def load: Properties = {
    val properties = new Properties()
    loadBaseProperties(properties)
    loadPropertiesFromClasspath(properties)
    properties
  }

  private[this] def loadBaseProperties(properties: Properties): Unit = {
    properties.load(
      getClass.getResourceAsStream("/com/twitter/finatra/validation/validation.properties"))
  }

  private[this] def loadPropertiesFromClasspath(properties: Properties): Unit = {
    val validationPropertiesUrl = getClass.getResource("/validation.properties")
    if (validationPropertiesUrl != null) {
      properties.load(validationPropertiesUrl.openStream())
    }
  }
}
