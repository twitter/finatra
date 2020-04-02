package com.twitter.finatra.validation.constraints;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.twitter.finatra.validation.Constraint;

/**
 * Per case class field annotation to validate if the value of the field exists in the set associated
 * with this annotation.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = OneOfConstraintValidator.class)
public @interface OneOf {

    /**
     * The array of valid values.
     */
    String[] value();
}
