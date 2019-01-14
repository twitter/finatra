package com.twitter.finatra.thrift.tests

import com.twitter.doeverything.thriftscala.{Answer, DoEverything}
import com.twitter.doeverything.thriftscala.DoEverything.{Ask, Echo, Echo2, MagicNum, MoreThanTwentyTwoArgs, Uppercase}
import com.twitter.finagle.{Filter, Service}
import com.twitter.finatra.thrift.Controller
import com.twitter.inject.Test
import com.twitter.scrooge.{Request, Response}
import com.twitter.util.Future

class ControllerTest extends Test {
  private val futureExn = Future.exception(new AssertionError("oh no"))

  test("Controller configuration is valid when all methods are specified") {
    val ctrl = new Controller(DoEverything) {
      handle(Ask) { args => futureExn }
      handle(MoreThanTwentyTwoArgs) { args: MoreThanTwentyTwoArgs.Args => futureExn }
      handle(MagicNum) { args: MagicNum.Args => futureExn }
      handle(Echo2) { args: Echo2.Args => futureExn }
      handle(Echo) { args: Echo.Args => futureExn }
      handle(Uppercase) { args: Uppercase.Args => futureExn }
    }
    ctrl.config match {
      case cc: Controller.ControllerConfig => assert(cc.isValid)
      case _ => fail("Configuration profile was incorrect")
    }
  }

  test("Controller configuration is invalid unless all methods are specified") {
    val ctrl = new Controller(DoEverything) {
      handle(Ask) { args => futureExn }
      handle(MoreThanTwentyTwoArgs) { args: MoreThanTwentyTwoArgs.Args => futureExn }
      handle(MagicNum) { args: MagicNum.Args => futureExn }
      // Missing Impl: handle(Echo2) { args: Echo2.Args => futureExn }
      handle(Echo) { args: Echo.Args => futureExn }
      handle(Uppercase) { args: Uppercase.Args => futureExn }
    }
    ctrl.config match {
      case cc: Controller.ControllerConfig => assert(!cc.isValid)
      case _ => fail("Configuration profile was incorrect")
    }
  }

  test("Controller configuration is invalid if more than one impl is given for a method") {
    val ctrl = new Controller(DoEverything) {
      handle(Ask) { args => futureExn }
      handle(MoreThanTwentyTwoArgs) { args: MoreThanTwentyTwoArgs.Args => futureExn }
      handle(MagicNum) { args: MagicNum.Args => futureExn }
      handle(Echo2) { args: Echo2.Args => futureExn }
      handle(Echo) { args: Echo.Args => futureExn }
      handle(Uppercase) { args: Uppercase.Args => futureExn }
      handle(Uppercase) { args: Uppercase.Args => futureExn }
    }
    ctrl.config match {
      case cc: Controller.ControllerConfig => assert(!cc.isValid)
      case _ => fail("Configuration profile was incorrect")
    }
  }

  test("When constructed in legacy mode, controller configuration is legacy config") {
    val ctrl = new Controller with DoEverything.BaseServiceIface {
      val uppercase: Service[Uppercase.Args, String] = handle(Uppercase) { args => futureExn }
      val echo: Service[Echo.Args, String] = handle(Echo) { args => futureExn }
      val echo2: Service[Echo2.Args, String] = handle(Echo2) { args => futureExn }
      val magicNum: Service[MagicNum.Args, String] = handle(MagicNum) { args => futureExn }
      val moreThanTwentyTwoArgs: Service[MoreThanTwentyTwoArgs.Args, String] = handle(MoreThanTwentyTwoArgs) { args => futureExn }
      val ask: Service[Ask.Args, Answer] = handle(Ask) { args => futureExn }
    }

    ctrl.config match {
      case lc: Controller.LegacyConfig =>
        assert(lc.methods.map(_.method).toSet.sameElements(DoEverything.methods))
      case _ => fail(s"Bad configuration ${ctrl.config}")
    }
  }

  test("Legacy controllers cannot use any MethodDSL functionality") {
    class TestController extends Controller with DoEverything.BaseServiceIface {
      val uppercase: Service[Uppercase.Args, String] = handle(Uppercase) { args => futureExn }
      val echo: Service[Echo.Args, String] = handle(Echo) { args => futureExn }
      val echo2: Service[Echo2.Args, String] = handle(Echo2) { args => futureExn }
      val magicNum: Service[MagicNum.Args, String] = handle(MagicNum) { args => futureExn }
      val moreThanTwentyTwoArgs: Service[MoreThanTwentyTwoArgs.Args, String] = handle(MoreThanTwentyTwoArgs) { args => futureExn }
      val ask: Service[Ask.Args, Answer] = handle(Ask) { args => futureExn }

      def check(): Boolean = {
        val dsl = handle(Echo)
        val fn = { req: Request[Echo.Args] =>
          Future.value(Response(req.args.msg))
        }

        intercept[AssertionError] {
          dsl.filtered(Filter.TypeAgnostic.Identity) { args: Echo.Args => Future.value(args.msg) }
        }

        intercept[AssertionError] {
          dsl.withFn(fn)
        }

        intercept[AssertionError] {
          dsl.withService(Service.mk(fn))
        }
        true
      }
    }

    assert(new TestController().check())
  }
}

