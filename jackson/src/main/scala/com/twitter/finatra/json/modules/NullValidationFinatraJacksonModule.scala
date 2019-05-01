package com.twitter.finatra.json.modules

/**
 * Provides a FinatraJacksonModule that will treat all fields as acceptable during
 * Finatra Validation of annotations.
 */
object NullValidationFinatraJacksonModule extends NullValidationFinatraJacksonModule

class NullValidationFinatraJacksonModule extends FinatraJacksonModule {

  override val finatraCaseClassModule = Some(NullValidationCaseClassModule)

}
