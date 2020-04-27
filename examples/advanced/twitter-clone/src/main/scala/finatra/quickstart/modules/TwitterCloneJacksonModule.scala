package finatra.quickstart.modules

import com.twitter.finatra.json.modules.FinatraJacksonModule

object TwitterCloneJacksonModule extends FinatraJacksonModule {
  override def numbersAsStrings = true
}