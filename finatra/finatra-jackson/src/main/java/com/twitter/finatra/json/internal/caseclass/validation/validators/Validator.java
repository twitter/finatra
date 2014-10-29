package com.twitter.finatra.json.internal.caseclass.validation.validators;

import com.twitter.finatra.json.ValidationResult;
import com.twitter.finatra.json.internal.caseclass.validation.ValidationMessageResolver;

public abstract class Validator<AnnotType, ValueType> {

    public Validator(
        ValidationMessageResolver validationMessageResolver,
        AnnotType annotation) {}

    public abstract ValidationResult isValid(ValueType value);
}
