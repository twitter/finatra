package com.twitter.finatra.thrift.routing

import com.twitter.finagle
import com.twitter.finagle._
import com.twitter.finagle.service.NilService
import com.twitter.finagle.thrift.AbstractThriftService
import com.twitter.finagle.thrift.RichServerParam
import com.twitter.finagle.thrift.ThriftService
import com.twitter.finagle.thrift.ToThriftService
import com.twitter.finatra.thrift.Controller
import com.twitter.finatra.thrift.ScroogeServiceImpl
import com.twitter.finatra.thrift.exceptions.ExceptionManager
import com.twitter.finatra.thrift.exceptions.ExceptionMapper
import com.twitter.finatra.thrift.internal.routing.NullThriftService
import com.twitter.finatra.thrift.internal.routing.Registrar
import com.twitter.inject.Injector
import com.twitter.inject.StackTransformer
import com.twitter.inject.TypeUtils
import com.twitter.inject.annotations.Flag
import com.twitter.inject.internal.LibraryRegistry
import com.twitter.scrooge.Request
import com.twitter.scrooge.Response
import com.twitter.scrooge.ThriftMethod
import com.twitter.util.logging.Logger
import java.lang.annotation.{Annotation => JavaAnnotation}
import java.lang.reflect.{Method => JMethod}
import javax.inject.Inject
import javax.inject.Singleton

private[routing] abstract class BaseThriftRouter[Router <: BaseThriftRouter[Router]](
  injector: Injector,
  exceptionManager: ExceptionManager) { this: Router =>

  // There is no guarantee that this is always accessed from the same thread
  @volatile
  private[this] var configurationComplete: Boolean = false

  // There is no guarantee that this is always accessed from the same thread
  @volatile
  protected[this] var serviceClazzStackParam: Option[Class[_]] = None

  /** Returns true is this router is configured */
  def isConfigured: Boolean = configurationComplete

  /** Returns the currently set `ServiceClassStackParam` value. */
  def getServiceClazzStackParam: Option[Class[_]] = serviceClazzStackParam

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
  def exceptionMapper[T <: Throwable](clazz: Class[_ <: ExceptionMapper[T, _]]): Router =
    preConfig("Exception mappers must be added before a controller is added") {
      val mapperType = TypeUtils.superTypeFromClass(clazz, classOf[ExceptionMapper[_, _]])
      val throwableType = TypeUtils.singleTypeParam(mapperType)
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

  /**
   * Ensure that `f` is only run prior to configuring a controller and setting up a thrift service.
   */
  protected def preConfig[T](what: String)(f: => T): T = {
    assert(!configurationComplete, what)
    f
  }

  /**
   * Ensure that `f` is only run after a controller has been configured
   */
  protected def postConfig[T](what: String)(f: => T): T = {
    assert(configurationComplete, what)
    f
  }

  /**
   * Ensures that configuring a controller happens only once and provides a consistent message
   */
  protected def assertController[T](f: => T): T = {
    val message =
      s"${this.getClass.getSimpleName}#add cannot be called multiple times, as we don't " +
        s"currently support serving multiple thrift services via the same router."

    val result = preConfig(message)(f)
    configurationComplete = true
    result
  }

  protected[this] def registerGlobalFilter(
    registry: LibraryRegistry,
    thriftFilter: Object
  ): Unit = {
    if (thriftFilter ne Filter.TypeAgnostic.Identity) {
      registry
        .withSection("thrift")
        .put("filters", thriftFilter.toString)
    }
  }

  protected[this] def registerService(
    registry: LibraryRegistry,
    serviceName: String
  ): Unit = {
    val name =
      if (serviceName.endsWith("$")) {
        serviceName.substring(0, serviceName.length - 1)
      } else serviceName
    registry
      .withSection("thrift")
      .put("service_class", name)
  }
}

private object ThriftRouter {
  val url: String =
    "https://twitter.github.io/finatra/user-guide/thrift/controllers.html#handle-thriftmethod-dsl"

  val logger: Logger = Logger(ThriftRouter.getClass)
}

/**
 * Builds a [[com.twitter.finagle.thrift.ThriftService]].
 *
 * @note this is only intended for use with generated Scala code. Users of generated Java code
 *       are encouraged to use the [[JavaThriftRouter]].
 */
@Singleton
class ThriftRouter @Inject() (
  injector: Injector,
  exceptionManager: ExceptionManager,
  stackTransformer: StackTransformer,
  @Flag("thrift.name") serverName: String)
    extends BaseThriftRouter[ThriftRouter](injector, exceptionManager) {
  import ThriftRouter._

  private[this] var underlying: ThriftService = NullThriftService

  // This map of routes is generated based on the controller and set once.
  private[this] var routes: Map[ThriftMethod, ScroogeServiceImpl] = _

  private[this] def filterStack[Req, Rep]: Stack[ServiceFactory[Req, Rep]] = {
    val nilStack = finagle.stack.nilStack[Req, Rep]
    val stackSvcFac = filters.foldLeft(nilStack) { (stack, filter) =>
      stack.prepend(Stack.Role(filter.toString), filter)
    }
    stackTransformer.apply(stackSvcFac)
  }

  private[this] var filters: Seq[Filter.TypeAgnostic] = Nil

  private[finatra] def routeWarmup[M <: ThriftMethod](
    m: M
  ): Service[Request[M#Args], Response[M#SuccessType]] =
    postConfig("Router has not been configured with a controller") {
      routes.get(m) match {
        case Some(s) => s.asInstanceOf[Service[Request[M#Args], Response[M#SuccessType]]]
        case None => throw new IllegalArgumentException(s"No route for method $m")
      }
    }

  /* Public */

  def thriftService: ThriftService =
    postConfig("Router has not been configured with a controller") {
      this.underlying
    }

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
  def filter[
    FilterType <: Filter.TypeAgnostic: Manifest,
    Ann <: JavaAnnotation: Manifest
  ]: ThriftRouter = {
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
  def filter(filter: Filter.TypeAgnostic): ThriftRouter =
    preConfig("'filter' must be called before 'add'.") {
      filters = filter +: filters
      this
    }

  /**
   * Instantiate and add thrift controller used for all requests.
   *
   * [[ThriftRouter]] only supports a single controller, so `add` may only be called once.
   *
   * @see the [[https://twitter.github.io/finatra/user-guide/thrift/controllers.html user guide]]
   */
  def add[C <: Controller: Manifest]: Unit = {
    val controller = injector.instance[C]
    add(controller)
  }

  /**
   * Add controller used for all requests. [[ThriftRouter]] only supports a single controller,
   * so `add` may only be called once.
   *
   * @see the [[https://twitter.github.io/finatra/user-guide/thrift/controllers.html user guide]]
   */
  def add(controller: Controller): Unit = {
    assertController {
      val reg = injector
        .instance[LibraryRegistry]
        .withSection("thrift", "methods")

      registerGlobalFilter(reg, filterStack)

      this.underlying = controller.config match {
        case c: Controller.ControllerConfig =>
          val controllerClazz = c.gen.getClass
          this.serviceClazzStackParam = Some(controllerClazz)
          registerService(reg, controllerClazz.getName)
          addController(controller, c)
        case c: Controller.LegacyConfig =>
          val controllerClazz = controller.getClass.getInterfaces.head
          this.serviceClazzStackParam = Some(controllerClazz)
          registerService(reg, controllerClazz.getName)
          addLegacyController(controller, c)
      }
    }
  }

  private[this] def addController(
    controller: Controller,
    conf: Controller.ControllerConfig
  ): ThriftService = {
    assert(
      conf.isValid, {
        val expectStr = conf.gen.methods.map(_.name).mkString("{", ", ", "}")
        val actualStr = conf.methods.map(_.method.name).mkString("{", ", ", "}")
        s"${controller.getClass.getSimpleName} for service " +
          s"${conf.gen.getClass.getSimpleName} is misconfigured. " +
          s"Expected exactly one implementation for each of $expectStr but found $actualStr"
      }
    )

    routes = conf.methods.map { cm =>
      val method: ThriftMethod = cm.method
      val service =
        cm.impl.asInstanceOf[Service[Request[method.Args], Response[method.SuccessType]]]
      thriftMethodRegistrar.register(controller.getClass, method, cm.filters)
      method -> {
        val endpoint = ServiceFactory.const(cm.filters.andThen(service))
        val stack = filterStack ++ Stack.leaf(finagle.stack.Endpoint, endpoint)
        val params = Stack.Params.empty +
          param.Label(serverName) +
          param.Tags(method.name, method.serviceName)
        val svcFac = stack.make(params)
        Service.pending(svcFac()).asInstanceOf[ScroogeServiceImpl]
      }
    }.toMap

    logger.info(
      "Adding methods\n" + routes.keys
        .map(method => s"${controller.getClass.getSimpleName}.${method.name}")
        .mkString("\n")
    )

    conf.gen.unsafeBuildFromMethods(routes).toThriftService
  }

  private[this] def addLegacyController(
    controller: Controller,
    conf: Controller.LegacyConfig
  ): ThriftService = {
    if (conf.methods.isEmpty) {
      logger.error(
        s"${controller.getClass.getName} contains no visible methods. " +
          s"For more details see: ${ThriftRouter.url}"
      )
    } else {
      routes = conf.methods.map { methodService =>
        val method = methodService.method
        thriftMethodRegistrar.register(controller.getClass, method, Filter.TypeAgnostic.Identity)
        methodService.setStack(filterStack)
        // Convert to a ScroogeServiceImpl for issuing warmup requests
        val castedService = methodService.asInstanceOf[Service[method.Args, method.SuccessType]]
        val reqRepService = Service.mk[Request[method.Args], Response[method.SuccessType]] { req =>
          castedService(req.args).map(Response[method.SuccessType])
        }
        method -> reqRepService.asInstanceOf[ScroogeServiceImpl]
      }.toMap

      logger.info(
        "Adding methods\n" + conf.methods
          .map(method => s"${controller.getClass.getSimpleName}.${method.name}")
          .mkString("\n")
      )
    }
    controller.asInstanceOf[ToThriftService].toThriftService
  }
}

private object JavaThriftRouter {
  val logger: Logger = Logger(JavaThriftRouter.getClass)
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
class JavaThriftRouter @Inject() (injector: Injector, exceptionManager: ExceptionManager)
    extends BaseThriftRouter[JavaThriftRouter](injector, exceptionManager) {
  import JavaThriftRouter._

  private class ServiceCreator {
    def apply(param: RichServerParam): Service[Array[Byte], Array[Byte]] = NilService
  }

  private[this] var serviceCreator: ServiceCreator = new ServiceCreator
  private[this] var filters: Filter.TypeAgnostic = Filter.TypeAgnostic.Identity

  /* Public */

  def createService(params: RichServerParam): Service[Array[Byte], Array[Byte]] =
    postConfig("Router has not been configured with a controller") {
      serviceCreator(params)
    }

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
  def filter[
    FilterType <: Filter.TypeAgnostic: Manifest,
    Ann <: JavaAnnotation: Manifest
  ]: JavaThriftRouter = {
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
  def filter(filter: Filter.TypeAgnostic): JavaThriftRouter =
    preConfig("'filter' must be called before add") {
      filters = filters.andThen(filter)
      this
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
  def add(
    controller: Class[_ <: AbstractThriftService]
  ): JavaThriftRouter = {
    add(injector.instance(controller))
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
  def add(
    controller: AbstractThriftService
  ): JavaThriftRouter = {
    def deriveServiceName(clazz: Class[_]): String = {
      val base = clazz.getName.stripSuffix("$" + "Service")
      base.substring(base.lastIndexOf(".") + 1)
    }

    serviceCreator = assertController {
      new ServiceCreator {
        override def apply(param: RichServerParam): Service[Array[Byte], Array[Byte]] = {
          val controllerClazz = controller.getClass
          val serviceIfaceClazz: Class[_] =
            controllerClazz.getInterfaces.head // MyService$ServiceIface
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
                controller.asInstanceOf[Object],
                filters,
                param
              )
              .asInstanceOf[Service[Array[Byte], Array[Byte]]]

          val declaredMethods: Array[JMethod] = controllerClazz.getDeclaredMethods
          logger.info(
            "Adding methods\n" +
              declaredMethods.map(method => s"$serviceName.${method.getName}").mkString("\n")
          )

          registerGlobalFilter(libraryRegistry, filters)
          serviceClazzStackParam = Some(serviceIfaceClazz)
          registerService(libraryRegistry, serviceClazz.getName)
          registerMethods(serviceName, controllerClazz, declaredMethods.toSeq)

          serviceInstance
        }
      }
    }

    this
  }

  /* Private */

  private[this] def registerMethods(
    serviceName: String,
    clazz: Class[_],
    methods: Seq[JMethod]
  ): Unit =
    methods.foreach(thriftMethodRegistrar.registerJavaMethod(serviceName, clazz, _))
}
