package com.twitter.finatra.http.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker {@link java.lang.annotation.Annotation} for denoting that the body of the Finagle HTTP
 * Message should not be parsed as a JSON body. Finagle HTTP requests with a content-type of
 * `application/json` sent to routes with a custom request case class callback input type will
 * always trigger the parsing of the request body as well-formed JSON in attempt to convert
 * the JSON into the request case class.This behavior can be disabled by annotating the case class
 * with `@JsonIgnoreBody` leaving the raw request body accessible.
 *
 * @see <a href="https://twitter.github.io/finatra/user-guide/http/requests.html#custom-request-case-class">Finatra User's Guide - HTTP Requests Custom Case Class</a>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonIgnoreBody {}
