package com.twitter.finatra.json.internal.caseclass.validation.validators;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.twitter.finatra.validation.Validation;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({ PARAMETER })
@Retention(RUNTIME)
@Validation(validatedBy = NotEmptyValidator.class)
public @interface NotEmptyInternal {

    // TODO Make configurable whether whitespace should be used in determining if value is nonempty
    // boolean ignoreWhitespace() default false
}
