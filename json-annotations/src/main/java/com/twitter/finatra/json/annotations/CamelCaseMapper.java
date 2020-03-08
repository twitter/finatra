package com.twitter.finatra.json.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.google.inject.BindingAnnotation;

/**
 * A <a href="https://github.com/google/guice/wiki">Google Guice</a>
 * {@link com.google.inject.BindingAnnotation} for binding an instance of a `ScalaObjectMapper`
 * configured with `PropertyNamingStrategy.LOWER_CAMEL_CASE` as a `PropertyNamingStrategy`.
 *
 * @see com.google.inject.BindingAnnotation
 * @see <a href="https://twitter.github.io/finatra/user-guide/getting-started/binding_annotations.html">Binding Annotations With Finatra</a>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@BindingAnnotation
public @interface CamelCaseMapper {}
