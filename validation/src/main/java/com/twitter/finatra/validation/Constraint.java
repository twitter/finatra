package com.twitter.finatra.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Define a validation constraint inspired by <a href="https://docs.oracle.com/javaee/6/tutorial/doc/gircz.html">JSR-303</a>
 * and <a href="https://jcp.org/en/jsr/detail?id=380">JSR-380</a> Bean Validation Specification.
 *<p>
 * Interfaces that use this annotation must override the validatedBy() field with a
 * {@code ConstraintValidator} that reference the annotation.
 *</p>
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Constraint {
  /**
   * Specify which validator to use
   */
  Class<? extends ConstraintValidator<?, ?>> validatedBy();
}
