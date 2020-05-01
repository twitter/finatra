package finatra.quickstart.modules

import com.twitter.finatra.jackson.modules.ScalaObjectMapperModule

object TwitterCloneJacksonModule extends ScalaObjectMapperModule {
  override def numbersAsStrings = true
}