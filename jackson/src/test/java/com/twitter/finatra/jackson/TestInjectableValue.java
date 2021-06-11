package com.twitter.finatra.jackson;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.twitter.util.jackson.annotation.InjectableValue;

/**
 * FOR TESTING ONLY
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@InjectableValue
public @interface TestInjectableValue {
  /**
   * FOR TESTING ONLY
   * @return the value
   */
  String value() default "";
}
