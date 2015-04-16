# Summary
finatra-jackson is a library integrating jackson, scala, and guice.

# Features
* Usable outside of finatra 
* FinatraObjectMapper which provides additional Scala friendly methods not found in ScalaObjectMapper.
* Guice module for injecting FinatraObjectMapper (with support for customization e.g. snake_case vs camelCase).
* Custom `case class` deserializer which overcomes limitations in jackson-scala-module.
* Support for `case class` validations which accumulate errors (without failing fast) during json parsing.
