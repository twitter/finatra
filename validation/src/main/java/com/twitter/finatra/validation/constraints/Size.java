package com.twitter.finatra.validation.constraints;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.twitter.finatra.validation.Constraint;

/**
 * Per case class field annotation to validate if the size of the field is within the min and max
 * value defined in the annotation.
 *
 * @Note The field must be of type Array, Traversable or a String.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SizeConstraintValidator.class)
public @interface Size {

    /**
     * The minimum size (inclusive)
     * @return the minimum size (inclusive)
     */
    long min();

    /**
     * The maximum size (inclusive)
     * @return the maximum size (inclusive)
     */
    long max();
}
