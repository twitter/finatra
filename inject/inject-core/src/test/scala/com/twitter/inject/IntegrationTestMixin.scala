package com.twitter.inject

import com.google.inject.Module
import com.google.inject.testing.fieldbinder.{Bind, BoundFieldModule}
import java.lang.reflect.Field
import org.mockito.internal.util.MockUtil
import org.scalatest.{Suite, SuiteMixin}

/**
 * Testing trait which extends the [[com.twitter.inject.TestMixin]] to provide
 * utilities for Integration testing with a test-defined [[com.twitter.inject.Injector]].
 *
 * This trait is expected to be mixed with a class that extends a core Suite trait,
 * e.g., [[org.scalatest.FunSuite]].
 *
 * While you can use this mixin directly, it is recommended that users extend
 * the [[com.twitter.inject.IntegrationTest]] abstract class.
 *
 * @see [[com.twitter.inject.TestMixin]]
 */
trait IntegrationTestMixin
  extends SuiteMixin
  with TestMixin { this: Suite =>

  /* Protected */

  protected def injector: Injector

  @deprecated("Users are encouraged to reset mocks and resettables directly as appropriate. See c.t.inject.Mockito#resetMocks and IntegrationTest#resetResettables", "2017-09-28")
  protected val resetBindings = true

  /** See https://github.com/google/guice/wiki/BoundFields */
  @deprecated("Use #bind[T] DSL instead.", "2017-03-01")
  protected val integrationTestModule: Module = BoundFieldModule.of(this)

  override protected def beforeAll() {
    super.beforeAll()
    injector.underlying.injectMembers(this)
  }

  override protected def afterEach() {
    super.afterEach()

    if (resetBindings) {
      for (mockObject <- mockObjects) {
        org.mockito.Mockito.reset(mockObject)
      }

      resetResettables(resettableObjects: _*)
    }
  }

  protected def resetResettables(resettables: Resettable*): Unit = {
    for (resettable <- resettables) {
      debug("Clearing " + resettable)
      resettable.reset()
    }
  }

  @deprecated("Use #bind[T] DSL instead.", "2017-03-01")
  protected def hasBoundFields: Boolean = boundFields.nonEmpty

  /* Private */

  @deprecated("Use #bind[T] DSL instead.", "2017-03-01")
  private[this] lazy val mockObjects = {
    val mockUtil = new MockUtil()
    for {
      field <- boundFields
      fieldValue = field.get(this)
      if mockUtil.isMock(fieldValue)
    } yield fieldValue
  }

  @deprecated("Users are encouraged to reset mocks and resettables directly as appropriate. See c.t.inject.Mockito#resetMocks and IntegrationTest#resetResettables", "2017-09-28")
  private[this] lazy val resettableObjects = {
    for {
      field <- boundFields
      if classOf[Resettable].isAssignableFrom(field.getType)
      _ = field.setAccessible(true)
      fieldValue = field.get(this)
    } yield fieldValue.asInstanceOf[Resettable]
  }

  @deprecated("Use #bind[T] DSL instead.", "2017-03-01")
  private[this] lazy val boundFields = {
    for {
      field <- getDeclaredFieldsRespectingInheritance(getClass)
      if hasBindAnnotation(field)
      _ = field.setAccessible(true)
    } yield field
  }

  @deprecated("Use #bind[T] DSL instead.", "2017-03-01")
  private def hasBindAnnotation(field: Field): Boolean = {
    field.getAnnotation(classOf[Bind]) != null
  }

  private def getDeclaredFieldsRespectingInheritance(clazz: Class[_]): Array[Field] = {
    if (clazz == null) {
      Array()
    } else {
      clazz.getDeclaredFields ++ getDeclaredFieldsRespectingInheritance(clazz.getSuperclass)
    }
  }
}
