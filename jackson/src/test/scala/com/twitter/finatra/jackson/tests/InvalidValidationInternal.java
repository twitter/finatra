package com.twitter.finatra.jackson.tests;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.twitter.finatra.validation.Validation;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * This class is in a circular dependency with the Scala class listed
 * in the `validatedBy` field and is thus included with the Scala test sources
 * for simplicity.
 */
@Target(PARAMETER)
@Retention(RUNTIME)
@Validation(validatedBy = InvalidValidator.class)
public @interface InvalidValidationInternal {
}
