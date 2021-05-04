package com.twitter.finatra.validation.constraints;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Per case class field annotation to validate if the value of the field is less than or equal to
 * the value associated with this annotation.
 *
 * @deprecated Prefer standard bean validation annotations
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MaxConstraintValidator.class)
@Deprecated
public @interface Max {

  /** message */
  String message() default "";

  /** groups */
  Class<?>[] groups() default {};

  /** payload */
  Class<? extends Payload>[] payload() default {};

  /**
   * The maximum value allowed on a field
   * @return the maximum value
   */
  long value();
}
