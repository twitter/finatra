package com.twitter.finatra.http.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

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
