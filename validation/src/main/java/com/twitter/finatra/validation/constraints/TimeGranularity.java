package com.twitter.finatra.validation.constraints;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

import com.twitter.finatra.validation.Constraint;

/**
 * Per case class field annotation to validate if the value of the field is of the time
 * granularity (days, hours, minutes, etc.) defined in the annotation.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TimeGranularityConstraintValidator.class)
public @interface TimeGranularity {

  /**
   * A time granularity
   * @return a time granularity
   */
  TimeUnit value();
}
