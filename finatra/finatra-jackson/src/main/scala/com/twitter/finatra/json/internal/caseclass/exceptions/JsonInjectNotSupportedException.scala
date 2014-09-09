package com.twitter.finatra.json.internal.caseclass.exceptions

import com.fasterxml.jackson.databind.JavaType

case class JsonInjectNotSupportedException(javaType: JavaType, fieldName: String)
  extends Exception("Injecting request attributes (e.g. QueryParam, Header, etc) not supported when explicitly calling " +
    "FinatraObjectMapper.parse for class " + javaType + " for field " + fieldName + ". Instead use a 'case class' input parameter on a Controller callback (e.g. get('/') ).")
