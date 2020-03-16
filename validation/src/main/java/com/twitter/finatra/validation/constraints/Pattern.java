package com.twitter.finatra.validation.constraints;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.twitter.finatra.validation.Constraint;

/**
 * Per case class field annotation to validate if the value of the field matches the specified
 * regular expression defined in the annotation.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PatternConstraintValidator.class)
public @interface Pattern {

    /**
     * The regular expression of a field
     * @return the regular expression to match
     */
    String regexp();
}
