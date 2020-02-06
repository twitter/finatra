package com.twitter.finatra.request;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.twitter.finatra.json.annotations.InjectableValue;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({ PARAMETER })
@Retention(RUNTIME)
@InjectableValue
public @interface QueryParam {
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
