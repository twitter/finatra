package com.twitter.finatra.validation.constraints;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Per case class field annotation to validate if the date time value of the field is in the future.
 *
 * @deprecated Prefer standard bean validation annotations
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = FutureTimeConstraintValidator.class)
@Deprecated
public @interface FutureTime {

  /** message */
  String message() default "";

  /** groups */
  Class<?>[] groups() default {};

  /** payload */
  Class<? extends Payload>[] payload() default {};
}
