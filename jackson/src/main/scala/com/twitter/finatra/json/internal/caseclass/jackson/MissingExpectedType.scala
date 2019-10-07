package com.twitter.finatra.json.internal.caseclass.jackson

private[finatra] class MissingExpectedType(clazz: Class[_])
    extends Error(
      "Parsed pickled Scala signature, but no expected type found: %s"
        .format(clazz)
    )
