package com.twitter.finatra.thrift.tests.doeverything.exceptions

import scala.util.control.NoStackTrace

class BarException extends Exception with NoStackTrace

class FooException extends Exception with NoStackTrace
