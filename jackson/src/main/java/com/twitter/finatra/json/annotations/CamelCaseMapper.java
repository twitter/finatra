package com.twitter.finatra.json.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

/**
 * A <a href="https://github.com/google/guice/wiki">Google Guice</a>
 * <a href="https://github.com/google/guice/wiki/BindingAnnotations>BindingAnnotation</a> for
 * binding an instance of a `ScalaObjectMapper` configured with `PropertyNamingStrategy.LOWER_CAMEL_CASE`.
 *
 * @see javax.inject.Qualifier
 * @see <a href="https://twitter.github.io/finatra/user-guide/getting-started/binding_annotations.html">Binding Annotations With Finatra</a>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Qualifier
public @interface CamelCaseMapper {}
