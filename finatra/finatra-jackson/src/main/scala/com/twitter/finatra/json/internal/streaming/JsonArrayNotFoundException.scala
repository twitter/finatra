package com.twitter.finatra.json.internal.streaming

class JsonArrayNotFoundException(val arrayName: String)
  extends Exception("JSON array with name '" + arrayName + "' not found")
