package com.twitter.finatra.validation.constraints;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Per case class field annotation to validate if the value of the field is not empty.
 *
 * @deprecated Prefer standard bean validation annotations
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NotEmptyConstraintValidator.class)
@Deprecated
public @interface NotEmpty {

  /** message */
  String message() default "";

  /** groups */
  Class<?>[] groups() default {};

  /** payload */
  Class<? extends Payload>[] payload() default {};

    // TODO Make configurable whether whitespace should be used in determining if value is nonempty
    // boolean ignoreWhitespace() default false
}
