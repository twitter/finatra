package com.twitter.finatra.http.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation for [[java.lang.annotation.Annotation]] interfaces which define
 * a `MessageBodyWriter` annotation. A `MessageBodyWriter` annotation is a
 * [[java.lang.annotation.Annotation]] used to annotate a class which should be rendered
 * by a registered `MessageBodyComponent` for the annotation type.
 *
 * @see [[https://twitter.github.io/finatra/user-guide/http/message_body.html Message Body Components]]
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MessageBodyWriter {}
