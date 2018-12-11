package com.twitter.finatra.thrift.tests.doeverything.exceptions

import com.twitter.finagle.ChannelException
import scala.util.control.NoStackTrace

class BarException extends Exception with NoStackTrace

class FooException extends Exception with NoStackTrace

class TestChannelException extends ChannelException with NoStackTrace
