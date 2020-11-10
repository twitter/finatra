package com.twitter.finatra.validation.tests;

import java.lang.annotation.Annotation;
import java.util.Collections;

import scala.reflect.ClassTag;

import org.junit.Assert;
import org.junit.Test;

import com.twitter.finatra.validation.MessageResolver;
import com.twitter.finatra.validation.ValidationException;
import com.twitter.finatra.validation.ValidationResult;
import com.twitter.finatra.validation.Validator;
import com.twitter.finatra.validation.Validator$;
import com.twitter.finatra.validation.ValidatorModule;
import com.twitter.finatra.validation.internal.AnnotatedClass;
import com.twitter.finatra.validation.internal.AnnotatedMethod;
import com.twitter.finatra.validation.internal.FieldValidator;
import com.twitter.inject.Injector;
import com.twitter.inject.app.TestInjector;

public class ValidatorJavaTest extends Assert {
  private final ValidatorModule validatorModule = ValidatorModule.get();
  private final MessageResolver messageResolver = new MessageResolver();

  /* Class under test */
  private final Validator validator = Validator.apply();
  /* Test Injector */
  private final Injector injector =
      TestInjector.apply(Collections.singletonList(validatorModule)).create();

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
    Assert.assertNotNull(
        Validator.builder()
            .withMessageResolver(messageResolver)
            .withCacheSize(128));
  }

  @Test
  public void testFromInjectorViaModule() {
    Assert.assertNotNull(injector.instance(Validator.class));
  }

  /* Test Builder style */
  @Test
  public void testWithMessageResolver() {
    class CustomizedMessageResolver extends MessageResolver {
      // 2.12 override
      // @Override omitted to present both overloads as examples
      public String resolve(Class<? extends Annotation> clazz,
        scala.collection.Seq<Object> values) {
          return "Whatever you provided is wrong.";
      }

      // 2.13 override
      // @Override omitted to present both overloads as examples
      @SuppressWarnings("unused")
      public String resolve(Class<? extends Annotation> clazz,
        scala.collection.immutable.Seq<Object> values) {
          return "Whatever you provided is wrong.";
      }

      // 2.12 override
      // @Override omitted to present both overloads as examples
      public <A extends Annotation> String resolve(
          scala.collection.Seq<Object> values,
          ClassTag<A> clazzTag) {
        return "Whatever you provided is wrong.";
      }

      // 2.13 override
      // @Override omitted to present both overloads as examples
      @SuppressWarnings("unused")
      public <A extends Annotation> String resolve(
          scala.collection.immutable.Seq<Object> values,
          ClassTag<A> clazzTag) {
        return "Whatever you provided is wrong.";
      }
    }

    CustomizedMessageResolver customizedMessageResolver = new CustomizedMessageResolver();
    Validator customizedValidator =
        Validator.builder().withMessageResolver(customizedMessageResolver).build();

    caseclasses.User testUser = new caseclasses.User("", "jack", "M");
    try {
      customizedValidator.verify(testUser);
    } catch (ValidationException e) {
      ValidationResult.Invalid result = e.errors().head();
      assertFalse(result.isValid());
      assertEquals("id: Whatever you provided is wrong.", result.message());
      assertEquals(
          getValidationAnnotations(testUser.getClass(), "id")[0],
          result.annotation().get());
    }
  }

  @Test
  public void testWithCacheSize() {
    Validator.Builder customizedValidatorBuilder = Validator.builder().withCacheSize(512);
    assertEquals(512, customizedValidatorBuilder.cacheSize());
  }

  /* Test validate() endpoint */
  @Test
  public void testValidatePassed() {
    caseclasses.User testUser = new caseclasses.User("1", "jack", "M");
    // valid result won't be returned
    try {
      validator.verify(testUser);
    } catch (ValidationException e) {
      assertEquals(0, e.errors().size());
    }
  }

  @Test
  public void testValidateFailed() {
    caseclasses.User testUser = new caseclasses.User("", "jack", "M");

    try {
      validator.verify(testUser);
    } catch (ValidationException e) {
      ValidationResult.Invalid result = e.errors().head();

      assertFalse(result.isValid());
      assertEquals("id: cannot be empty", result.message());
      assertEquals(
          getValidationAnnotations(testUser.getClass(), "id")[0],
          result.annotation().get());
    }
  }

  @Test
  public void testUserDefinedConstraint() {
    caseclasses.StateValidationExample stateValidationExample =
        new caseclasses.StateValidationExample("NY");
    try {
      validator.verify(stateValidationExample);
    } catch (ValidationException e) {
      ValidationResult.Invalid result = e.errors().head();
      assertFalse(result.isValid());
      assertEquals("state: Please register with state CA", result.message());
      assertEquals(
          getValidationAnnotations(stateValidationExample.getClass(), "state")[0],
          result.annotation().get());
    }
  }

  /* Test validateField() endpoint */
  @Test
  public void testValidateFieldPassed() {
    caseclasses.MinIntExample minIntExample = new caseclasses.MinIntExample(10);
    FieldValidator[] fieldValidators = {
        validator
            .findFieldValidator(
                getValidationAnnotations(minIntExample.getClass(), "numberValue")[0])
    };
    // valid result won't be returned
    Assert.assertEquals(0, validator.validateField(2, "numberValue", fieldValidators).size());
  }

  @Test
  public void testValidateFieldFailed() {
    caseclasses.MinIntExample minIntExample = new caseclasses.MinIntExample(0);
    FieldValidator[] fieldValidators = {
        validator
            .findFieldValidator(
                getValidationAnnotations(minIntExample.getClass(), "numberValue")[0])
    };
    ValidationResult result = validator.validateField(0, "numberValue", fieldValidators).head();
    assertFalse(result.isValid());
    assertEquals(
        getValidationAnnotations(minIntExample.getClass(), "numberValue")[0],
        result.annotation().get());
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
            getMethodValidations(testUser.getClass())
        ).size()
    );
  }

  @Test
  public void testValidateMethodFailed() throws NoSuchMethodException {
    caseclasses.User testUser = new caseclasses.User("1", "", "M");
    ValidationResult.Invalid result =
        (ValidationResult.Invalid) validator.validateMethods(
            testUser,
            getMethodValidations(testUser.getClass())
        ).head();
    assertFalse(result.isValid());
    assertEquals("cannot be empty", result.message());
    assertEquals(
        getValidationAnnotations(testUser.getClass(), "nameCheck")[0],
        result.annotation().get()
    );
  }

  /* Utils */
  private Annotation[] getValidationAnnotations(Class<?> clazz, String name) {
    AnnotatedClass annotatedClass = validator.findAnnotatedClass(clazz);
    return annotatedClass.getAnnotationsForAnnotatedMember(name);
  }

  private AnnotatedMethod[] getMethodValidations(Class<?> clazz) {
    return Validator$.MODULE$.getMethodValidations(clazz);
  }
}
