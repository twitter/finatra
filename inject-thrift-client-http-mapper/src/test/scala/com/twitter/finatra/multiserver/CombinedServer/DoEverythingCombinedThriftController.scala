package com.twitter.finatra.multiserver.CombinedServer

import com.twitter.adder.thriftscala.Adder
import com.twitter.adder.thriftscala.Adder.{Add1AlwaysError, Add1Slowly, Add1, Add1String}
import com.twitter.finatra.thrift.Controller
import com.twitter.util.Future
import javax.inject.{Inject, Singleton}

@Singleton
class DoEverythingCombinedThriftController @Inject()(
    adder: AdderService)
  extends Controller
  with Adder.BaseServiceIface {

  override val add1 = handle(Add1) { args: Add1.Args =>
    Future(adder.add1(args.num))
  }

  override val add1String = handle(Add1String) { args: Add1String.Args =>
    Future(adder.add1String(args.num))
  }

  override val add1Slowly = handle(Add1Slowly) { args: Add1Slowly.Args =>
    Thread.sleep(5000)
    Future((args.num.toInt + 1).toString)
  }

  override val add1AlwaysError = handle(Add1AlwaysError) { args: Add1AlwaysError.Args =>
    Future.exception(new RuntimeException("oops"))
  }
}
