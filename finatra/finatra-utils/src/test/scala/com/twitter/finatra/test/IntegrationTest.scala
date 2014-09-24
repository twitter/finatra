package com.twitter.finatra.test

import com.google.inject.testing.fieldbinder.{Bind, BoundFieldModule}
import com.twitter.finatra.utils.Clearable
import java.lang.reflect.Field
import org.mockito.Mockito
import org.mockito.internal.util.MockUtil

/** See https://github.com/google/guice/wiki/BoundFields */
trait IntegrationTest extends Test {

  /* Protected */

  protected def app: EmbeddedApp

  protected val resetMocks = true
  protected val resetClearables = true

  protected val integrationTestModule = BoundFieldModule.of(this)

  override protected def beforeAll() {
    super.beforeAll()

    assert(app.isGuiceApp)
    app.start()
    app.injector.guiceInjector.injectMembers(this)
  }

  override protected def afterEach() {
    super.afterEach()

    if (resetMocks) {
      for (mockObject <- mockObjects) {
        println("Reseting " + mockObject)
        Mockito.reset(mockObject)
      }
    }

    if (resetClearables) {
      for (clearableObject <- clearableObjects) {
        println("Clearing " + clearableObject)
        clearableObject.clear()
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

  private lazy val clearableObjects = {
    for {
      field <- boundFields
      if classOf[Clearable].isAssignableFrom(field.getType)
      _ = field.setAccessible(true)
      fieldValue = field.get(this)
    } yield fieldValue.asInstanceOf[Clearable]
  }

  private lazy val boundFields = {
    for {
      field <- getClass.getDeclaredFields
      if hasBindAnnotation(field)
      _ = field.setAccessible(true)
    } yield field
  }

  private def hasBindAnnotation(field: Field): Boolean = {
    field.getAnnotation(classOf[Bind]) != null
  }
}