package com.twitter.finatra.http.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marker {@link java.lang.annotation.Annotation} for denoting `MessageBodyComponents`
 * which should be written by an associated `MessageBodyWriter`. In this
 * case, it is expected that annotated components will be written by
 * the associated `MustacheMessageBodyWriter`.
 *
 * @see com.twitter.finatra.http.annotations.MessageBodyWriter
 * @see <a href="https://twitter.github.io/finatra/user-guide/mustache/index.html">Finatra User's Guide - Mustache Support</a>
 * @see <a href="https://twitter.github.io/finatra/user-guide/http/message_body.html#messagebodywriter-annotation">Finatra User's Guide - HTTP Message Body Writer Annotation</a>
 */
@Target(PARAMETER)
@Retention(RUNTIME)
@MessageBodyWriter
public @interface Mustache {
  /**
   * Template name
   */
  String value();

  /**
   *
   * The value to be set for the HTTP response's Content-Type header
   * e.g. "text/html; charset=utf-8"
   *      "application/json; charset=utf-8"
   */
  String contentType() default "text/html; charset=utf-8";
}
