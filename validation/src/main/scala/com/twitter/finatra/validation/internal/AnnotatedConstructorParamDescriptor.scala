package com.twitter.finatra.validation.internal

import java.lang.annotation.Annotation
import org.json4s.reflect.ConstructorParamDescriptor

private[validation] case class AnnotatedConstructorParamDescriptor(
  param: ConstructorParamDescriptor,
  parameterizedTypeNames: Array[String],
  annotations: Array[Annotation])
