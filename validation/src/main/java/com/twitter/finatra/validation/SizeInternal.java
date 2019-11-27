package com.twitter.finatra.validation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target(PARAMETER)
@Retention(RUNTIME)
@Validation(validatedBy = SizeValidator.class)
public @interface SizeInternal {

    /**
     * The minimum size (inclusive)
     * @return the minimum size (inclusive)
     */
    long min();

    /**
     * The maximum size (inclusive)
     * @return the maximum size (inclusive)
     */
    long max();
}
