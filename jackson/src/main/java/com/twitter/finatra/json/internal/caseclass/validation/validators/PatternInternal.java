package com.twitter.finatra.json.internal.caseclass.validation.validators;

import com.twitter.finatra.validation.Validation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({PARAMETER})
@Retention(RUNTIME)
@Validation(validatedBy = PatternValidator.class)
public @interface PatternInternal {

    /**
     * @return the regular expression to match
     */
    String regexp();
}
