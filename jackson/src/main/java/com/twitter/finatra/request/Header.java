package com.twitter.finatra.request;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.twitter.finatra.json.annotations.InjectableValue;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({ PARAMETER })
@Retention(RUNTIME)
@InjectableValue
public @interface Header {
    String value() default "";
}
