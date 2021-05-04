package com.twitter.finatra.validation.constraints;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Per case class field annotation to validate if the value of the field is a 2-letter country code
 * defined in ISO 3166.
 *
 * @see <a href="https://www.iso.org/iso-3166-country-codes.html">ISO 3166 Spec</a>
 *
 * @deprecated Prefer standard bean validation annotations
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CountryCodeConstraintValidator.class)
@Deprecated
public @interface CountryCode {

  /** message */
  String message() default "";

  /** groups */
  Class<?>[] groups() default {};

  /** payload */
  Class<? extends Payload>[] payload() default {};
}
