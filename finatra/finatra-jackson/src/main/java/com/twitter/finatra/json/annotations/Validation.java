package com.twitter.finatra.json.annotations;

import com.twitter.finatra.json.internal.caseclass.validation.validators.Validator;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({ ANNOTATION_TYPE })
@Retention(RUNTIME)
public @interface Validation {
	Class<? extends Validator<?, ?>> validatedBy();
}

