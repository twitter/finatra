package com.twitter.finatra.validation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target(PARAMETER)
@Retention(RUNTIME)
@Validation(validatedBy = PatternValidator.class)
public @interface PatternInternal {

    /**
     * The regular expression of a field
     * @return the regular expression to match
     */
    String regexp();
}
