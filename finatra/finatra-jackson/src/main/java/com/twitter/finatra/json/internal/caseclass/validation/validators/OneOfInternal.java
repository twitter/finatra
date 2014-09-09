package com.twitter.finatra.json.internal.caseclass.validation.validators;

import com.twitter.finatra.json.internal.caseclass.validation.Validation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({ PARAMETER })
@Retention(RUNTIME)
@Validation(validatedBy = OneOfValidator.class)
public @interface OneOfInternal {

    /**
     * The array of valid values.
     */
    String[] value();
}
