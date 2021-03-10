package com.twitter.finatra.http;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.twitter.finatra.http.annotations.MessageBodyWriter;

/**
 * FOR TESTING ONLY
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@MessageBodyWriter
public @interface TestMessageBodyWriterAnn {}
