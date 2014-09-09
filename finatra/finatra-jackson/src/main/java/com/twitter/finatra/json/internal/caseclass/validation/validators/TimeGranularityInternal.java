package com.twitter.finatra.json.internal.caseclass.validation.validators;

import com.twitter.finatra.json.internal.caseclass.validation.Validation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({ PARAMETER })
@Retention(RUNTIME)
@Validation(validatedBy = TimeGranularityValidator.class)
public @interface TimeGranularityInternal {

  /**
   * @return a time granularity
   */
  TimeUnit value();
}
