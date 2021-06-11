package com.twitter.finatra.http.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.twitter.util.jackson.annotation.InjectableValue;

/**
 * Marker {@link java.lang.annotation.Annotation} for denoting a Jackson "injectable value" which
 * should be obtained from a Finagle HTTP Request URI path variable. This specifically allows
 * access to the Finatra "named parameter" of the path of an HTTP route callback defined in a
 * Controller.
 *
 * @see com.twitter.util.jackson.annotation.InjectableValue
 * @see <a href="https://twitter.github.io/finatra/user-guide/json/index.html#injectablevalues">Finatra User's Guide - JSON Injectable Values</a>
 * @see <a href="https://twitter.github.io/finatra/user-guide/http/controllers.html#named-parameters">Finatra User's Guide - Controller Named Parameters</a>
 * @see <a href="https://twitter.github.io/finatra/user-guide/http/requests.html#field-annotations">Finatra User's Guide - HTTP Request Field Annotations</a>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@InjectableValue
public @interface RouteParam {
    /**
     * An optional field name to use for reading the route parameter from the Finagle HTTP Request.
     * When empty, the annotated case class field name will be used to read the route parameter
     * from the Finagle HTTP Request parameters.
     * @return the name of the route parameter field.
     */
    String value() default "";
}
