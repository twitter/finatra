package com.twitter.finatra.http;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.twitter.finatra.http.annotations.MessageBodyWriter;

@Target(PARAMETER)
@Retention(RUNTIME)
@MessageBodyWriter
public @interface TestMessageBodyWriterAnn {
}
