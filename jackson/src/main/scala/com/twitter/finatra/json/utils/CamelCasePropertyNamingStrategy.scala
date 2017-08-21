package com.twitter.finatra.json.utils

import com.fasterxml.jackson.databind.PropertyNamingStrategy

/* By default PropertyNamingStrategy uses CamelCase */
@deprecated(
  "This was needed with previous versions of Jackson in which the " +
    "PropertyNamingStrategy was an abstract class with a non-obvious " +
    "default of camel case. Please use " +
    "PropertyNamingStrategy#LOWER_CAMEL_CASE directly.",
  "2017-08-15"
)
object CamelCasePropertyNamingStrategy extends PropertyNamingStrategy
