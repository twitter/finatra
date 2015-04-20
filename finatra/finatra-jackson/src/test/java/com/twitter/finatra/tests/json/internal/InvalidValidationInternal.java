package com.twitter.finatra.tests.json.internal;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.twitter.finatra.validation.InvalidValidator;
import com.twitter.finatra.validation.Validation;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target(PARAMETER)
@Retention(RUNTIME)
@Validation(validatedBy = InvalidValidator.class)
public @interface InvalidValidationInternal {

}
