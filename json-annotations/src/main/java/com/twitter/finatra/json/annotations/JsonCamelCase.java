package com.twitter.finatra.json.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Override "default" SnakeCase deserialization
 * NOTE: Serialization not currently affected
 *
 * @deprecated no replacement, please use @JsonProperty, a configured PropertyNamingStrategy or
 * the `com.fasterxml.jackson.databind.annotation.JsonNaming` annotation.
 * @see "com.fasterxml.jackson.databind.annotation.JsonNaming"
 * @see <a href="http://fasterxml.github.io/jackson-databind/javadoc/2.2.0/com/fasterxml/jackson/databind/annotation/JsonNaming.html">@JsonNaming</a>
 */
@Target(TYPE)
@Retention(RUNTIME)
@Deprecated
public @interface JsonCamelCase {}
