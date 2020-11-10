package com.twitter.finatra.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a property, method parameter or method return type for validation cascading.
 * <p>
 * Constraints defined on the object and its properties are validated when the
 * property, method parameter or method return type is validated.
 * <p>
 * This behavior is applied recursively.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Valid {}
