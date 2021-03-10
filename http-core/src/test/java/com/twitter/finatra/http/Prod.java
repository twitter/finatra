package com.twitter.finatra.http;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.google.inject.BindingAnnotation;

/**
 * FOR TESTING ONLY
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@BindingAnnotation
public @interface Prod {}
