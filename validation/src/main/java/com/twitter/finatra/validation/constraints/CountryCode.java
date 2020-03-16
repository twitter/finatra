package com.twitter.finatra.validation.constraints;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.twitter.finatra.validation.Constraint;

/**
 * Per case class field annotation to validate if the value of the field is a 2-letter country code
 * defined in ISO 3166.
 *
 * @see <a href="https://www.iso.org/iso-3166-country-codes.html">ISO 3166 Spec</a>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CountryCodeConstraintValidator.class)
public @interface CountryCode {}
