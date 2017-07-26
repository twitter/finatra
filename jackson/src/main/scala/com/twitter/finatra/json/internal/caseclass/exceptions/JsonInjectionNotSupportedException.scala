package com.twitter.finatra.json.internal.caseclass.exceptions

import scala.language.existentials

case class JsonInjectionNotSupportedException(parentClass: Class[_], fieldName: String)
    extends Exception(
      "Injection of fields (e.g. @JsonInject, @QueryParam, @Header) not " +
        "supported when parsing using a mapper created with FinatraObjectMapper.create() for " +
        "" + fieldName + " in class " + parentClass
    )
