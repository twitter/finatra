package com.twitter.finatra.validation;

public abstract class Validator<AnnotType, ValueType> {

    public Validator(
        ValidationMessageResolver validationMessageResolver,
        AnnotType annotation) {}

    public abstract ValidationResult isValid(ValueType value);
}
