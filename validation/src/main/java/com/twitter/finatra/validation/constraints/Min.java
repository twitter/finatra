package com.twitter.finatra.validation.constraints;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.twitter.finatra.validation.Constraint;

/**
 * Per case class field annotation to validate if the value of the field is greater than or equal to
 * the value associated with this annotation.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MinConstraintValidator.class)
public @interface Min {

  /**
   * The minimum value allowed on a field
   * @return the minimum value
   */
  long value();
}
