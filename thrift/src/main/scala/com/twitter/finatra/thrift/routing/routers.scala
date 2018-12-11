package com.twitter.finatra.thrift.routing

import com.twitter.finagle.service.NilService
import com.twitter.finagle.thrift.{RichServerParam, ThriftService, ToThriftService}
import com.twitter.finagle.{Filter, Service, Thrift, ThriftMux}
import com.twitter.finatra.thrift._
import com.twitter.finatra.thrift.exceptions.{ExceptionManager, ExceptionMapper}
import com.twitter.finatra.thrift.internal.ThriftMethodService
import com.twitter.finatra.thrift.internal.routing.{NullThriftService, Registrar}
import com.twitter.inject.TypeUtils._
import com.twitter.inject.internal.LibraryRegistry
import com.twitter.inject.{Injector, Logging}
import com.twitter.scrooge.ThriftMethod
import java.lang.reflect.{Method => JMethod}
import java.lang.annotation.{Annotation => JavaAnnotation}
import javax.inject.{Inject, Singleton}
import org.apache.thrift.protocol.TProtocolFactory
import scala.collection.mutable.{Map => MutableMap}

private[routing] abstract class BaseThriftRouter[Router <: BaseThriftRouter[Router]](
  injector: Injector,
  exceptionManager: ExceptionManager
) extends Logging { this: Router =>

  private[this] var done: Boolean = false

  /**
   * Add exception mapper used for the corresponding exceptions.
   *
   * @see the [[https://twitter.github.io/finatra/user-guide/thrift/exceptions.html user guide]]
   */
  def exceptionMapper[T <: ExceptionMapper[_, _]: Manifest]: Router = {
    exceptionManager.add[T]
    this
  }

  /**
   * Add exception mapper used for the corresponding exceptions.
   *
   * @see the [[https://twitter.github.io/finatra/user-guide/thrift/exceptions.html user guide]]
   */
  def exceptionMapper[T <: Throwable: Manifest](mapper: ExceptionMapper[T, _]): Router = {
    exceptionManager.add[T](mapper)
    this
  }

  /**
   * Add exception mapper used for the corresponding exceptions.
   *
   * @see the [[https://twitter.github.io/finatra/user-guide/thrift/exceptions.html user guide]]
   */
  def exceptionMapper[T <: Throwable](clazz: Class[_ <: ExceptionMapper[T, _]]): Router = {
    val mapperType = superTypeFromClass(clazz, classOf[ExceptionMapper[_, _]])
    val throwableType = singleTypeParam(mapperType)
    exceptionMapper(injector.instance(clazz))(
      Manifest.classType(Class.forName(throwableType.getTypeName))
    )
    this
  }

  /* Protected */

  protected lazy val libraryRegistry: LibraryRegistry =
    injector.instance[LibraryRegistry]

  protected lazy val thriftMethodRegistrar: Registrar =
    new Registrar(
      libraryRegistry
        .withSection("thrift", "methods")
    )

  protected def assertController(f: => Unit): Unit = {
    assert(
      !done,
      s"${this.getClass.getSimpleName}#add cannot be called multiple times, as we don't " +
        s"currently support serving multiple thrift services via the same router."
    )
    f
    done = true
  }

  protected[this] def registerGlobalFilter(thriftFilter: Filter.TypeAgnostic): Unit = {
    if (thriftFilter ne Filter.TypeAgnostic.Identity) {
      libraryRegistry
        .withSection("thrift")
        .put("filters", thriftFilter.toString)
    }
  }
}

private object ThriftRouter {
  val url: String =
    "https://twitter.github.io/finatra/user-guide/thrift/controllers.html#handle-thriftmethod-dsl"
}

/**
 * Builds a [[com.twitter.finagle.thrift.ThriftService]].
 *
 * @note this is only intended for use with generated Scala code. Users of generated Java code
 *       are encouraged to use the [[JavaThriftRouter]].
 */
@Singleton
class ThriftRouter @Inject()(injector: Injector, exceptionManager: ExceptionManager)
    extends BaseThriftRouter[ThriftRouter](injector, exceptionManager) {

  private[this] var underlying: ThriftService = NullThriftService
  protected[this] var filters: Filter.TypeAgnostic = Filter.TypeAgnostic.Identity

  private[finatra] val methods = MutableMap[ThriftMethod, ThriftMethodService[_, _]]()

  /* Public */

  def thriftService: ThriftService = this.underlying

  def thriftMethodService(method: ThriftMethod): ThriftMethodService[_, _] = this.methods(method)

  /**
   * Add global filter used for all requests.
   *
   * The filter is appended after other `Filters` that have already been added
   * via `filter`.
   *
   * @see The [[https://twitter.github.io/finatra/user-guide/thrift/filters.html user guide]]
   */
  def filter[FilterType <: Filter.TypeAgnostic: Manifest]: ThriftRouter = {
    filter(injector.instance[FilterType])
  }

  /**
   * Add global filter used for all requests that are annotated with Annotation Type.
   *
   * The filter is appended after other `Filters` that have already been added
   * via `filter`.
   *
   * @see The [[https://twitter.github.io/finatra/user-guide/thrift/filters.html user guide]]
   */
  def filter[FilterType <: Filter.TypeAgnostic: Manifest, Ann <: JavaAnnotation: Manifest]
    : ThriftRouter = {
    filter(injector.instance[FilterType, Ann])
  }

  /**
   * Add global filter used for all requests.
   *
   * The filter is appended after other `Filters` that have already been added
   * via `filter`.
   *
   * @see The [[https://twitter.github.io/finatra/user-guide/thrift/filters.html user guide]]
   */
  def filter(clazz: Class[_ <: Filter.TypeAgnostic]): ThriftRouter = {
    filter(injector.instance(clazz))
  }

  /**
   * Add global filter used for all requests.
   *
   * The filter is appended after other `Filters` that have already been added
   * via `filter`.
   *
   * @see The [[https://twitter.github.io/finatra/user-guide/thrift/filters.html user guide]]
   */
  def filter(filter: Filter.TypeAgnostic): ThriftRouter = {
    assert(underlying == NullThriftService, "'filter' must be called before 'add'.")
    filters = filters.andThen(filter)
    this
  }

  /**
   * Instantiate and add thrift controller used for all requests.
   *
   * [[ThriftRouter]] only supports a single controller, so `add` may only be called once.
   *
   * @see the [[https://twitter.github.io/finatra/user-guide/thrift/controllers.html user guide]]
   */
  def add[C <: Controller with ToThriftService: Manifest]: ThriftRouter = {
    val controller = injector.instance[C]
    add(controller)
  }

  /**
   * Add controller used for all requests. [[ThriftRouter]] only supports a single controller,
   * so `add` may only be called once.
   *
   * @see the [[https://twitter.github.io/finatra/user-guide/thrift/controllers.html user guide]]
   */
  def add(controller: Controller with ToThriftService): ThriftRouter = {
    assertController {
      if (controller.methods.isEmpty) {
        error(
          s"${controller.getClass.getName} contains no visible methods. For more details see: ${ThriftRouter.url}"
        )
      } else {
        for (m <- controller.methods) {
          m.setFilter(filters)
          methods += (m.method -> m)
        }
        info(
          "Adding methods\n" + controller.methods
            .map(method => s"${controller.getClass.getSimpleName}.${method.name}")
            .mkString("\n")
        )
      }
      registerMethods(controller.getClass, controller.methods.map(_.method))
      registerGlobalFilter(filters)
      underlying = controller.toThriftService
    }
    this
  }

  private[this] def registerMethods(
    clazz: Class[_],
    methods: Seq[ThriftMethod]
  ): Unit =
    methods.foreach(thriftMethodRegistrar.register(clazz, _))

  private[this] def registerGlobalFilter(thriftFilter: Filter.TypeAgnostic): Unit = {
    if (thriftFilter ne Filter.TypeAgnostic.Identity) {
      libraryRegistry
        .withSection("thrift")
        .put("filters", thriftFilter.toString)
    }
  }
}

/**
 * Builds a [[com.twitter.finagle.Service]].
 *
 * A [[ThriftRouter]] specifically for use with generated Java code. Users of generated Scala code
 * should use the [[com.twitter.finatra.thrift.routing.ThriftRouter]] directly.
 *
 * @note routing over Java generated code DOES NOT support per-method stats since the generated
 *       Java code does not yet support "service-per-method".
 *
 * @see [[com.twitter.finatra.thrift.routing.ThriftRouter]]
 * @see [[com.twitter.finatra.thrift.routing.BaseThriftRouter]]
 */
@Singleton
class JavaThriftRouter @Inject()(injector: Injector, exceptionManager: ExceptionManager)
    extends BaseThriftRouter[JavaThriftRouter](injector, exceptionManager) {

  private[this] var underlying: Service[Array[Byte], Array[Byte]] = NilService
  private[this] var filters: Filter.TypeAgnostic = Filter.TypeAgnostic.Identity

  /* Public */

  def service: Service[Array[Byte], Array[Byte]] = this.underlying

  /**
   * Add global filter used for all requests.
   *
   * The filter is appended after other `Filters` that have already been added
   * via `filter`.
   *
   * @see The [[https://twitter.github.io/finatra/user-guide/thrift/filters.html user guide]]
   */
  def filter[FilterType <: Filter.TypeAgnostic: Manifest]: JavaThriftRouter = {
    this.filter(injector.instance[FilterType])
  }

  /**
   * Add global filter used for all requests that are annotated with Annotation Type.
   *
   * The filter is appended after other `Filters` that have already been added
   * via `filter`.
   *
   * @see The [[https://twitter.github.io/finatra/user-guide/thrift/filters.html user guide]]
   */
  def filter[FilterType <: Filter.TypeAgnostic: Manifest, Ann <: JavaAnnotation: Manifest]
    : JavaThriftRouter = {
    this.filter(injector.instance[FilterType, Ann])
  }

  /**
   * Add global filter used for all requests.
   *
   * The filter is appended after other `Filters` that have already been added
   * via `filter`.
   *
   * @see The [[https://twitter.github.io/finatra/user-guide/thrift/filters.html user guide]]
   */
  def filter(clazz: Class[_ <: Filter.TypeAgnostic]): JavaThriftRouter = {
    this.filter(injector.instance(clazz))
  }

  /**
   * Add global filter used for all requests.
   *
   * The filter is appended after other `Filters` that have already been added
   * via `filter`.
   *
   * @see The [[https://twitter.github.io/finatra/user-guide/thrift/filters.html user guide]]
   */
  def filter(filter: Filter.TypeAgnostic): JavaThriftRouter = {
    assert(underlying == NilService, "'filter' must be called before 'add'.")
    filters = filters.andThen(filter)
    this
  }

  /**
   * Add controller used for all requests for usage from Java. The [[ThriftRouter]] only supports
   * a single controller, so `add` may only be called once.
   *
   * @see the [[https://twitter.github.io/finatra/user-guide/thrift/controllers.html user guide]]
   */
  def add(controller: Class[_]): JavaThriftRouter = {
    add(controller, ThriftMux.server.params.apply[Thrift.param.ProtocolFactory].protocolFactory)
  }

  /**
   * Add controller used for all requests for usage from Java. The [[ThriftRouter]] only supports a
   * single controller, so `add` may only be called once.
   *
   * @note We do not apply filters per-method but instead all filters are applied across the service.
   *       thus "per-method" metrics will be scoped to the controller name (Class#getSimpleName).
   *
   * @see the [[https://twitter.github.io/finatra/user-guide/thrift/controllers.html user guide]]
   */
  def add(controller: Class[_], protocolFactory: TProtocolFactory): JavaThriftRouter = {
    def deriveServiceName(clazz: Class[_]): String = {
      val base = clazz.getName.stripSuffix("$" + "Service")
      base.substring(base.lastIndexOf(".") + 1)
    }

    assertController {
      val controllerInstance = injector.instance(controller)
      val serviceIfaceClazz: Class[_] =
        controllerInstance.getClass.getInterfaces.head // MyService$ServiceIface
      val serviceClazz: Class[_] = // MyService$Service
        // note, the $ gets concat-ed strangely to avoid a false positive scalac warning
        // for "possible missing interpolator".
        Class.forName(serviceIfaceClazz.getName.stripSuffix("$ServiceIface") + "$" + "Service")
      val serviceConstructor =
        serviceClazz.getConstructor(
          serviceIfaceClazz,
          classOf[Filter.TypeAgnostic],
          classOf[RichServerParam]
        )

      val serviceName = deriveServiceName(serviceClazz)

      // instantiate service
      val serviceInstance: Service[Array[Byte], Array[Byte]] =
        serviceConstructor
          .newInstance(
            controllerInstance.asInstanceOf[Object],
            filters,
            new RichServerParam(protocolFactory)
          )
          .asInstanceOf[Service[Array[Byte], Array[Byte]]]

      val declaredMethods: Array[JMethod] = controller.getDeclaredMethods
      info(
        "Adding methods\n" +
          declaredMethods.map(method => s"$serviceName.${method.getName}").mkString("\n")
      )

      registerGlobalFilter(filters)
      registerMethods(serviceName, controller, declaredMethods.toSeq)
      underlying = serviceInstance
    }
    this
  }

  /* Private */

  private[this] def registerMethods(
    serviceName: String,
    clazz: Class[_],
    methods: Seq[JMethod]
  ): Unit =
    methods.foreach(thriftMethodRegistrar.register(serviceName, clazz, _))
}
