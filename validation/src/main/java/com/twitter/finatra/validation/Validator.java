package com.twitter.finatra.validation;

public abstract class Validator<AT, VT> {

    public Validator(
        ValidationMessageResolver validationMessageResolver,
        AT annotation) { }

    /**
     * Determine if a field is valid or not
     * @param value The value to be validated
     * @return A `ValidationResult`
     */
    public abstract ValidationResult isValid(VT value);
}
