package com.twitter.finatra.validation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target(PARAMETER)
@Retention(RUNTIME)
@Validation(validatedBy = TimeGranularityValidator.class)
public @interface TimeGranularityInternal {

  /**
   * A time granularity
   * @return a time granularity
   */
  TimeUnit value();
}
