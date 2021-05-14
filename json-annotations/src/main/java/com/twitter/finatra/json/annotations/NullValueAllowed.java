package com.twitter.finatra.json.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation that defines a field for which a null is a valid value.
 *
 * This does not change the optionality of the field.
 * Mandatory fields still must have a value, but the value can be a null.
 *
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NullValueAllowed {}
