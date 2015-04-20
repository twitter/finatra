package com.twitter.finatra.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.google.inject.BindingAnnotation;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotates fields parsed from TwitterServer flags.
 * TODO: Move out of finatra package
 */
@Retention(RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@BindingAnnotation
public @interface Flag {

    /**
     * Name of flag
     */
    String value();
}
