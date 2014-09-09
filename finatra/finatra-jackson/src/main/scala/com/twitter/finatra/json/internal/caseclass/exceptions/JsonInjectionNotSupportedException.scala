package com.twitter.finatra.json.internal.caseclass.exceptions

//TODO: Improve error msg
case class JsonInjectionNotSupportedException(parentClass: Class[_], fieldName: String)
  extends Exception("Injection of fields (e.g. @JsonInject, @QueryParam, @Header) not supported when parsing using mapper created with FinatraObjectMapper.create or json._ implicits for " + fieldName + " into " + parentClass)
