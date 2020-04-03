package com.twitter.finatra.validation.tests;

import java.lang.annotation.Annotation;
import java.util.ArrayList;

import scala.collection.Seq;

import org.junit.Assert;
import org.junit.Test;

import com.twitter.finatra.validation.MessageResolver;
import com.twitter.finatra.validation.ValidationException;
import com.twitter.finatra.validation.ValidationResult;
import com.twitter.finatra.validation.Validator;
import com.twitter.finatra.validation.internal.AnnotatedClass;
import com.twitter.finatra.validation.internal.AnnotatedField;
import com.twitter.finatra.validation.internal.FieldValidator;

public class ValidatorJavaTest extends Assert {
  private MessageResolver messageResolver = new MessageResolver();
  private Validator validator = Validator.apply();

  /* Test Constructors */
  @Test
  public void testConstructors() {
    Assert.assertNotNull(Validator.apply());
    Assert.assertNotNull(Validator.apply(messageResolver));
  }

  @Test
  public void testBuilderConstructors() {
    Assert.assertNotNull(Validator.builder().withMessageResolver(messageResolver));
    Assert.assertNotNull(Validator.builder().withCacheSize(512));
  }

  /* Test Builder style */
  @Test
  public void testWithMessageResolver() {
    class CustomizedMessageResolver extends MessageResolver {
      // 2.12 override
      // @Override omitted to present both overloads as examples
      public String resolve(Class<? extends Annotation> clazz, scala.collection.Seq<Object> values) {
        return "Whatever you provided is wrong.";
      }

      // 2.13 override
      // @Override omitted to present both overloads as examples
      public String resolve(Class<? extends Annotation> clazz, scala.collection.immutable.Seq<Object> values) {
        return "Whatever you provided is wrong.";
      }
    }

    CustomizedMessageResolver customizedMessageResolver = new CustomizedMessageResolver();
    Validator customizedValidator =
        Validator.builder().withMessageResolver(customizedMessageResolver).build();

    caseclasses.User testUser = new caseclasses.User("", "jack", "M");
    try {
      customizedValidator.validate(testUser);
    } catch (ValidationException e) {
      ValidationResult.Invalid result = e.errors().head();
      assertFalse(result.isValid());
      assertEquals(result.message(), "Whatever you provided is wrong.");
      assertEquals(
          result.annotation().get(),
          getValidationAnnotations(testUser.getClass(), "id").get(0)
      );
    }
  }

  @Test
  public void testWithCacheSize() {
    Validator.Builder customizedValidatorBuilder = Validator.builder().withCacheSize(512);
    assertEquals(customizedValidatorBuilder.cacheSize(), 512);
  }

  /* Test validate() endpoint */
  @Test
  public void testValidatePassed() {
    caseclasses.User testUser = new caseclasses.User("1", "jack", "M");
    // valid result won't be returned
    try {
      validator.validate(testUser);
    } catch (ValidationException e) {
      assertEquals(0, e.errors().size());
    }
  }

  @Test
  public void testValidateFailed() {
    caseclasses.User testUser = new caseclasses.User("", "jack", "M");

    try {
      validator.validate(testUser);
    } catch (ValidationException e) {
      ValidationResult.Invalid result = e.errors().head();

      assertFalse(result.isValid());
      assertEquals(result.message(), "cannot be empty");
      assertEquals(
          result.annotation().get(),
          getValidationAnnotations(testUser.getClass(), "id").get(0)
      );
    }
  }

  @Test
  public void testUserDefinedConstraint() {
    caseclasses.StateValidationExample stateValidationExample =
        new caseclasses.StateValidationExample("NY");
    try {
      validator.validate(stateValidationExample);
    } catch (ValidationException e) {
      ValidationResult.Invalid result = e.errors().head();
      assertFalse(result.isValid());
      assertEquals(result.message(), "Please register with state CA");
      assertEquals(
          result.annotation().get(),
          getValidationAnnotations(stateValidationExample.getClass(), "state").get(0)
      );
    }
  }

  /* Test validateField() endpoint */
  @Test
  public void testValidateFieldPassed() {
    caseclasses.MinIntExample minIntExample = new caseclasses.MinIntExample(10);
    FieldValidator[] fieldValidators = {
        validator
            .findFieldValidator(getValidationAnnotations(minIntExample.getClass(), "numberValue")
            .get(0))
    };
    // valid result won't be returned
    Assert.assertEquals(0, validator.validateField(2, fieldValidators).size());
  }

  @Test
  public void testValidateFieldFailed() {
    caseclasses.MinIntExample minIntExample = new caseclasses.MinIntExample(0);
    FieldValidator[] fieldValidators = {
        validator
            .findFieldValidator(getValidationAnnotations(minIntExample.getClass(), "numberValue")
            .get(0))
    };
    ValidationResult result = validator.validateField(0, fieldValidators).head();
    assertFalse(result.isValid());
    assertEquals(
        result.annotation().get(),
        getValidationAnnotations(minIntExample.getClass(), "numberValue").get(0)
    );
  }

  /* Test validateMethod() endpoint */
  @Test
  public void testValidateMethodPassed() {
    caseclasses.User testUser = new caseclasses.User("1", "jack", "M");
    // valid result won't be returned
    Assert.assertEquals(
        0,
        validator.validateMethods(
            testUser,
            validator.getMethodValidations(testUser.getClass())
        ).size()
    );
  }

  @Test
  public void testValidateMethodFailed() throws NoSuchMethodException {
    caseclasses.User testUser = new caseclasses.User("1", "", "M");
    ValidationResult.Invalid result =
        (ValidationResult.Invalid) validator.validateMethods(
            testUser,
            validator.getMethodValidations(testUser.getClass())
        ).head();
    assertFalse(result.isValid());
    assertEquals(result.message(), "name cannot be empty");
    assertEquals(
        result.annotation().get(),
        testUser.getClass().getMethod("nameCheck").getAnnotations()[0]
    );
  }

  /* Utils */
  private ArrayList<Annotation> getValidationAnnotations(Class<?> clazz, String name) {
    AnnotatedClass annotatedClass = validator.getAnnotatedClass(clazz);
    AnnotatedField field = annotatedClass.fields().get(name).get();
    FieldValidator[] fieldValidators = field.fieldValidators();
    ArrayList<Annotation> annotations = new ArrayList<>();
    for (FieldValidator fieldValidator : fieldValidators) {
      annotations.add(fieldValidator.annotation());
    }
    return annotations;
  }
}
