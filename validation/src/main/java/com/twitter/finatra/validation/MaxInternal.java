package com.twitter.finatra.validation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target(PARAMETER)
@Retention(RUNTIME)
@Validation(validatedBy = MaxValidator.class)
public @interface MaxInternal {

  /**
   * The maximum value allowed on a field
   * @return the maximum value
   */
  long value();
}
