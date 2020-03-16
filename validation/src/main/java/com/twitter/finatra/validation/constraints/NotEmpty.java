package com.twitter.finatra.validation.constraints;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.twitter.finatra.validation.Constraint;

/**
 * Per case class field annotation to validate if the value of the field is not empty.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NotEmptyConstraintValidator.class)
public @interface NotEmpty {

    // TODO Make configurable whether whitespace should be used in determining if value is nonempty
    // boolean ignoreWhitespace() default false
}
