package com.twitter.finatra.validation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target(PARAMETER)
@Retention(RUNTIME)
@Validation(validatedBy = RangeValidator.class)
public @interface RangeInternal {

    /**
     * The minimum value of a range (inclusive)
     * @return the minimum value (inclusive)
     */
    long min();

    /**
     * The maximum value of a range (inclusive)
     * @return the maximum value (inclusive)
     */
    long max();
}
