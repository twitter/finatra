package com.twitter.finatra.json.tests
import com.twitter.finatra.jackson.tests.AbstractScalaObjectMapperTest
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.json.modules.FinatraJacksonModule
import com.twitter.inject.Injector
import com.twitter.inject.app.TestInjector

@deprecated("See the ScalaObjectMapperFromInjectorTest", "2019-12-10")
class FinatraObjectMapperFromInjectorTest extends AbstractScalaObjectMapperTest {
  private[this] val mapperModule: FinatraJacksonModule = FinatraJacksonModule

  /* Test Injector */
  private[this] final val injector: Injector = TestInjector(mapperModule).create
  /* Class under test */
  override protected val mapper: FinatraObjectMapper = injector.instance[FinatraObjectMapper]
}
