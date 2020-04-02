package com.twitter.finatra.validation.constraints;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.twitter.finatra.validation.Constraint;

/**
 * Per case class field annotation to validate if the date time value of the field is in the future.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = FutureTimeConstraintValidator.class)
public @interface FutureTime {}
