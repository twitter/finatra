package com.twitter.finatra.validation.constraints;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Per case class field annotation to validate if the value of the field is of the time
 * granularity (days, hours, minutes, etc.) defined in the annotation.
 *
 * @deprecated Prefer standard bean validation annotations
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TimeGranularityConstraintValidator.class)
@Deprecated
public @interface TimeGranularity {

  /** message */
  String message() default "";

  /** groups */
  Class<?>[] groups() default {};

  /** payload */
  Class<? extends Payload>[] payload() default {};

  /**
   * A time granularity
   * @return a time granularity
   */
  TimeUnit value();
}
