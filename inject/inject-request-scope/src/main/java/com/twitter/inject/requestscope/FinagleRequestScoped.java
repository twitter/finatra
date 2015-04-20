package com.twitter.inject.requestscope;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.google.inject.ScopeAnnotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({TYPE, METHOD})
@Retention(RUNTIME)
@ScopeAnnotation
public @interface FinagleRequestScoped {
}
