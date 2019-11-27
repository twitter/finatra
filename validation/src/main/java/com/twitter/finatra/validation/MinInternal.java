package com.twitter.finatra.validation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target(PARAMETER)
@Retention(RUNTIME)
@Validation(validatedBy = MinValidator.class)
public @interface MinInternal {

  /**
   * The minimum value allowed on a field
   * @return value the minimum value
   */
  long value();
}
