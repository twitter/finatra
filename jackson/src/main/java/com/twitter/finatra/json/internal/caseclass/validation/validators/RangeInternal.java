package com.twitter.finatra.json.internal.caseclass.validation.validators;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.twitter.finatra.validation.Validation;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({ PARAMETER })
@Retention(RUNTIME)
@Validation(validatedBy = RangeValidator.class)
public @interface RangeInternal {

    /**
     * @return the minimum value (inclusive)
     */
    long min();

    /**
     * @return the maximum value (inclusive)
     */
    long max();
}
