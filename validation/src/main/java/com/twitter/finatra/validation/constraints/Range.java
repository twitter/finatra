package com.twitter.finatra.validation.constraints;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Per case class field annotation to validate if the value of the field is within the min and max
 * value defined in the annotation.
 *
 * @deprecated Prefer standard bean validation annotations
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RangeConstraintValidator.class)
@Deprecated
public @interface Range {

    /** message */
    String message() default "";

    /** groups */
    Class<?>[] groups() default {};

    /** payload */
    Class<? extends Payload>[] payload() default {};

    /**
     * The minimum value of a range (inclusive)
     * @return the minimum value (inclusive)
     */
    long min();

    /**
     * The maximum value of a range (inclusive)
     * @return the maximum value (inclusive)
     */
    long max();
}
