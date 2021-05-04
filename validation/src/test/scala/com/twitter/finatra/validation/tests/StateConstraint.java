package com.twitter.finatra.validation.tests;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = { StateConstraintValidator.class })
public @interface StateConstraint {

  /** */
  String message() default "Please register with state CA";

  /** */
  Class<?>[] groups() default {};

  /** */
  Class<? extends Payload>[] payload() default {};
}
