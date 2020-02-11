package com.twitter.finatra.http.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.twitter.finatra.json.annotations.InjectableValue;

/**
 * Marker {@link java.lang.annotation.Annotation} for denoting a Jackson "injectable value" which
 * should be obtained from a Finagle HTTP Request query string parameter.
 *
 * @see com.twitter.finatra.json.annotations.InjectableValue
 * @see <a href="https://twitter.github.io/finatra/user-guide/json/index.html#injectablevalues">Finatra User's Guide - JSON Injectable Values</a>
 * @see <a href="https://twitter.github.io/finatra/user-guide/http/requests.html#field-annotations">Finatra User's Guide - HTTP Request Field Annotations</a>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@InjectableValue
public @interface QueryParam {
    /**
     * An optional field name to use for reading the query parameter from the Finagle HTTP Request.
     * When empty, the annotated case class field name will be used to read the query parameter
     * from the Finagle HTTP Request parameters.
     * @return the name of the query parameter field.
     */
    String value() default "";

    /**
     * Set to true for parameters that are in comma-separated
     * format, so that they will be split before parsing the
     * individual values. Only meaningful for collections.
     *
     * This makes it easy to support the query param styles
     * from RFC-6570 (URI Template) Section 3.2.8
     *   https://tools.ietf.org/html/rfc6570#section-3.2.8
     *
     * These styles use the modifiers defined elsewhere in the spec, especially
     * the "explode modifier" defined in Section 2.4.2
     *   https://tools.ietf.org/html/rfc6570#section-2.4.2
     *
     * commaSeparatedList == false corresponds to the "explode modifier" style
     *
     *   {&list*}           &list=red&list=green&list=blue
     *
     * commaSeparatedList == true corresponds to the non-"explode modifier" style:
     *
     *   {&list}            &list=red,green,blue
     */
    boolean commaSeparatedList() default false;
}
