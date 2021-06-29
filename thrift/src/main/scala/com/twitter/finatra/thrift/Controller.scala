package com.twitter.finatra.thrift

import com.twitter.finagle.thrift.{GeneratedThriftService, ToThriftService}
import com.twitter.finagle.{Filter, Service}
import com.twitter.finatra.thrift.internal.ThriftMethodService
import com.twitter.inject.Logging
import com.twitter.scrooge.{Request, Response, ThriftMethod}
import com.twitter.util.Future
import scala.collection.mutable.ListBuffer

private[thrift] object Controller {
  case class ConfiguredMethod(
    method: ThriftMethod,
    filters: Filter.TypeAgnostic,
    impl: ScroogeServiceImpl)

  sealed trait Config

  @deprecated("Construct controllers with a GeneratedThriftService", "2018-12-20")
  class LegacyConfig extends Config {
    val methods = new ListBuffer[ThriftMethodService[_, _]]
  }

  class ControllerConfig(val gen: GeneratedThriftService) extends Config {
    val methods = new ListBuffer[ConfiguredMethod]

    def isValid: Boolean = {
      val expected = gen.methods
      methods.size == expected.size && methods.map(_.method).toSet == expected
    }
  }
}

abstract class Controller private (val config: Controller.Config) extends Logging { self =>
  import Controller._

  @deprecated("Construct controllers with a GeneratedThriftService", "2018-12-20")
  def this() {
    this(new Controller.LegacyConfig)
    assert(self.isInstanceOf[ToThriftService], "Legacy controllers must extend a service iface")
  }

  def this(gen: GeneratedThriftService) {
    this(new Controller.ControllerConfig(gen))
    assert(
      !self.isInstanceOf[ToThriftService],
      "Controllers should no longer extend ToThriftService")
  }

  /**
   * The MethodDSL child class is responsible for capturing the state of the applied filter chains
   * and implementation.
   */
  class MethodDSL[M <: ThriftMethod](val m: M, chain: Filter.TypeAgnostic) {

    private[this] def nonLegacy[T](f: ControllerConfig => T): T = {
      assert(config.isInstanceOf[ControllerConfig], "Legacy controllers cannot use method DSLs")
      f(config.asInstanceOf[ControllerConfig])
    }

    /**
     * Add a filter to the implementation
     */
    def filtered(f: Filter.TypeAgnostic): MethodDSL[M] = nonLegacy { _ =>
      new MethodDSL[M](m, chain.andThen(f))
    }

    /**
     * Provide an implementation for this method in the form of a [[com.twitter.finagle.Service]]
     *
     * @note The service will be called for each request.
     *
     * @param svc the service to use as an implementation
     */
    def withService(svc: Service[Request[M#Args], Response[M#SuccessType]]): Unit = nonLegacy {
      cc =>
        cc.methods += ConfiguredMethod(m, chain, svc.asInstanceOf[ScroogeServiceImpl])
    }

    /**
     * Provide an implementation for this method in the form of a function of
     * Request => Future[Response].
     *
     * @note The given function will be invoked for each request.
     *
     * @param fn the function to use
     */
    def withFn(fn: Request[M#Args] => Future[Response[M#SuccessType]]): Unit = nonLegacy { cc =>
      withService(Service.mk(fn))
    }

    /**
     * Provide an implementation for this method in the form a function of Args => Future[SuccessType]
     * This exists for legacy compatibility reasons. Users should instead use Request/Response
     * based functionality.
     *
     * @note The implementation given will be invoked for each request.
     *
     * @param f the implementation
     * @return a ThriftMethodService, which is used in legacy controller configurations
     */
    @deprecated("Use Request/Response based functionality", "2018-12-20")
    def apply(f: M#Args => Future[M#SuccessType]): ThriftMethodService[M#Args, M#SuccessType] = {
      config match {
        case _: ControllerConfig =>
          withService(Service.mk { req: Request[M#Args] =>
            f(req.args).map(Response[M#SuccessType])
          })

          // This exists to match return types with the legacy methods of creating a controller.
          // The service created here should never be invoked.
          new ThriftMethodService[M#Args, M#SuccessType](
            m,
            Service.mk { _ =>
              throw new RuntimeException("Legacy shim service invoked")
            })

        case lc: LegacyConfig =>
          val thriftMethodService = new ThriftMethodService[M#Args, M#SuccessType](m, Service.mk(f))
          lc.methods += thriftMethodService
          thriftMethodService

      }
    }
  }

  /**
   * Have the controller handle a thrift method with optionally applied filters and an
   * implementation. All thrift methods that a ThriftService handles must be registered using
   * this method to properly construct a Controller.
   *
   * @note The provided implementation will be invoked for each request.
   *
   * @param m The thrift method to handle.
   *
   */
  protected def handle[M <: ThriftMethod](m: M) = new MethodDSL[M](m, Filter.TypeAgnostic.Identity)
}
