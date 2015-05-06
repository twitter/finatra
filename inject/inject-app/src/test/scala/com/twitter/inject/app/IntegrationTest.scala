package com.twitter.inject.app

import com.google.inject.testing.fieldbinder.{Bind, BoundFieldModule}
import com.twitter.inject.Test
import java.lang.reflect.Field
import org.mockito.Mockito
import org.mockito.internal.util.MockUtil

/** See https://github.com/google/guice/wiki/BoundFields */
trait IntegrationTest extends Test {

  /* Protected */

  protected def app: EmbeddedApp

  protected val resetBindings = true

  protected val integrationTestModule = BoundFieldModule.of(this)

  override protected def beforeAll() {
    super.beforeAll()

    assert(app.isGuiceApp)
    app.start()
    app.injector.underlying.injectMembers(this)
  }

  override protected def afterEach() {
    super.afterEach()

    if (resetBindings) {
      for (mockObject <- mockObjects) {
        Mockito.reset(mockObject)
      }

      for (resettable <- resettableObjects) {
        debug("Clearing " + resettable)
        resettable.reset()
      }
    }
  }

  /* Private */

  private lazy val mockObjects = {
    val mockUtil = new MockUtil()
    for {
      field <- boundFields
      fieldValue = field.get(this)
      if mockUtil.isMock(fieldValue)
    } yield fieldValue
  }

  private lazy val resettableObjects = {
    for {
      field <- boundFields
      if classOf[Resettable].isAssignableFrom(field.getType)
      _ = field.setAccessible(true)
      fieldValue = field.get(this)
    } yield fieldValue.asInstanceOf[Resettable]
  }

  private lazy val boundFields = {
    for {
      field <- getDeclaredFieldsRespectingInheritance(getClass)
      if hasBindAnnotation(field)
      _ = field.setAccessible(true)
    } yield field
  }

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