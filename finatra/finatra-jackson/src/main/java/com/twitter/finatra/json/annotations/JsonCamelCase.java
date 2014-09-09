package com.twitter.finatra.json.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Override "default" SnakeCase deserialization
 * NOTE: Serialization not currently affected
 */
@Target({TYPE})
@Retention(RUNTIME)
public @interface JsonCamelCase {

}