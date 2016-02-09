package com.twitter.finatra.multiserver.CombinedServer

import com.twitter.adder.thriftscala.Adder
import com.twitter.adder.thriftscala.Adder.{Add1, Add1String}
import com.twitter.finatra.thrift.Controller
import com.twitter.util.Future
import javax.inject.{Inject, Singleton}

@Singleton
class DoEverythingCombinedThriftController @Inject()(
    adder: AdderService)
  extends Controller
  with Adder.BaseServiceIface {

  override val add1 = handle(Add1) { args: Add1.Args =>
    Future.value(adder.add1(args.num))
  }

  override val add1String = handle(Add1String) { args: Add1String.Args =>
    Future.value(adder.add1String(args.num))
  }
}
