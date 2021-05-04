package com.twitter.finatra.validation.constraints;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Per case class field annotation to validate if the size of the field is within the min and max
 * value defined in the annotation.
 *
 * @implNote The field must be of type Array, Traversable or a String.
 *
 * @deprecated Prefer standard bean validation annotations
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SizeConstraintValidator.class)
@Deprecated
public @interface Size {

    /** message */
    String message() default "";

    /** groups */
    Class<?>[] groups() default {};

    /** payload */
    Class<? extends Payload>[] payload() default {};

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
