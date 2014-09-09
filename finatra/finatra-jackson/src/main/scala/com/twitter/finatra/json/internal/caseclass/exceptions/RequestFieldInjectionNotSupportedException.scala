package com.twitter.finatra.json.internal.caseclass.exceptions

class RequestFieldInjectionNotSupportedException
  extends Exception(
    "Injecting request attributes (e.g. QueryParam, Header, etc) not supported when explicitly calling " +
      "FinatraObjectMapper.parse. Instead use a 'case class' input parameter on a Controller callback " +
      "(e.g. get('/') { r: ClassWithRequestAttributes => ... } ).")
