package com.twitter.inject.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Annotates App/Server lifecycle methods.
 */
@Retention(SOURCE)
@Target(ElementType.METHOD)
public @interface Lifecycle {
}
