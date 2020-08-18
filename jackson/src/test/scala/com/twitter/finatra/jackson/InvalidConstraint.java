package com.twitter.finatra.jackson;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.twitter.finatra.validation.Constraint;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = InvalidConstraintValidator.class)
public @interface InvalidConstraint {
}
