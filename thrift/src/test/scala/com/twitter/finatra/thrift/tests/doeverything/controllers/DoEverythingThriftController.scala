package com.twitter.finatra.thrift.tests.doeverything.controllers

import com.twitter.doeverything.thriftscala.DoEverything
import com.twitter.doeverything.thriftscala.DoEverything.{Echo, MagicNum, MoreThanTwentyTwoArgs, Uppercase}
import com.twitter.finatra.thrift.Controller
import com.twitter.inject.annotations.Flag
import com.twitter.util.Future
import javax.inject.Inject

class DoEverythingThriftController @Inject()(
  @Flag("magicNum") magicNumValue: String)
  extends Controller
  with DoEverything.BaseServiceIface {

  override val uppercase = handle(Uppercase) { args: Uppercase.Args =>
    if (args.msg == "fail") {
      Future.exception(new Exception("oops"))
    } else {
      Future.value(args.msg.toUpperCase)
    }
  }

  override val echo = handle(Echo) { args: Echo.Args =>
    Future.value(args.msg)
  }

  override val magicNum = handle(MagicNum) { args: MagicNum.Args =>
    Future.value(magicNumValue)
  }

  override val moreThanTwentyTwoArgs = handle(MoreThanTwentyTwoArgs) { args : MoreThanTwentyTwoArgs.Args =>
    Future.value("handled")
  }


}
