# Summary
finatra-jackson is a library integrating jackson, scala, and guice.

# Features
* Usable outside of finatra (and will become ***REMOVED***'s default json renderer in the future). 
* FinatraObjectMapper which provides additional Scala friendly methods not found in ScalaObjectMapper.
* Guice module for injecting FinatraObjectMapper (with support for customization e.g. snake_case vs camelCase).
* Custom "case class" deserializer which overcomes limitations in jackson-scala-module.
* Support for "case class" validations which accumulate errors (without failing fast) during json parsing.
* Integration with Finatra HTTP routing to support binding and validation of query params, route params, and headers.
* Utils for comparing json in tests.
* Experimental support for iterator based json stream parsing.

# Links
***REMOVED***
***REMOVED***
***REMOVED***
***REMOVED***
***REMOVED***
***REMOVED***
***REMOVED***
