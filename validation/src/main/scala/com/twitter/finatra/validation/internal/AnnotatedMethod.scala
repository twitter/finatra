package com.twitter.finatra.validation.internal

import com.twitter.finatra.validation.MethodValidation
import java.lang.reflect.Method

/**
 * A Finatra internal class that carries a [[Method]] along with its [[MethodValidation]] annotation.
 */
private[finatra] case class AnnotatedMethod(method: Method, annotation: MethodValidation)
