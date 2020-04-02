package com.twitter.finatra.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to annotate a case class method for validating fields of case classes.
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MethodValidation {
  /**
   * Specify the fields to validate
   */
  String[] fields() default "";
}

