package com.twitter.finatra.http.tests.integration.doeverything.main.exceptions

import scala.util.control.NoStackTrace

class FooException(val id: String)
  extends Exception
  with NoStackTrace

class BarException extends FooException("123")

class BazException extends FooException("321")
