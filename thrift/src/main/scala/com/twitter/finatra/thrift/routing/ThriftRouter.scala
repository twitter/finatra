package com.twitter.finatra.thrift.routing

import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.{Service, Thrift, ThriftMux}
import com.twitter.finagle.thrift.{ThriftService, ToThriftService}
import com.twitter.finatra.thrift._
import com.twitter.finatra.thrift.exceptions.{ExceptionManager, ExceptionMapper}
import com.twitter.finatra.thrift.internal.routing.{NullThriftService, Services, Registrar}
import com.twitter.finatra.thrift.internal.{ThriftMethodService, ThriftRequestUnwrapFilter, ThriftRequestWrapFilter}
import com.twitter.inject.TypeUtils._
import com.twitter.inject.internal.LibraryRegistry
import com.twitter.inject.{Injector, Logging}
import com.twitter.scrooge.ThriftMethod
import java.lang.annotation.{Annotation => JavaAnnotation}
import java.lang.reflect.Method
import javax.inject.{Inject, Singleton}
import org.apache.thrift.protocol.TProtocolFactory
import scala.collection.mutable.{Map => MutableMap}
import scala.reflect.ClassTag

private object ThriftRouter {
  val url: String =
    "https://twitter.github.io/finatra/user-guide/thrift/controllers.html#handle-thriftmethod-dsl"
}

@Singleton
class ThriftRouter @Inject()(
  exceptionManager: ExceptionManager,
  statsReceiver: StatsReceiver,
  injector: Injector
) extends Logging {

  private var filterChain = ThriftFilter.Identity
  private var done = false
  private var filteredThriftService: ThriftService = NullThriftService
  private var filteredService: Option[_ <: Service[Array[Byte], Array[Byte]]] = None

  private[finatra] var name: String = ""
  private[finatra] val methods = MutableMap[ThriftMethod, ThriftMethodService[_, _]]()
  private[finatra] lazy val services: Services = {
    Services(filteredService, filteredThriftService)
  }

  /* Public */

  /**
   * Add exception mapper used for the corresponding exceptions.
   *
   * @see the [[https://twitter.github.io/finatra/user-guide/thrift/exceptions.html user guide]]
   */
  def exceptionMapper[T <: ExceptionMapper[_, _]: Manifest]: ThriftRouter = {
    exceptionManager.add[T]
    this
  }

  /**
   * Add exception mapper used for the corresponding exceptions.
   *
   * @see the [[https://twitter.github.io/finatra/user-guide/thrift/exceptions.html user guide]]
   */
  def exceptionMapper[T <: Throwable: Manifest](mapper: ExceptionMapper[T, _]): ThriftRouter = {
    exceptionManager.add[T](mapper)
    this
  }

  /**
   * Add exception mapper used for the corresponding exceptions.
   *
   * @see the [[https://twitter.github.io/finatra/user-guide/thrift/exceptions.html user guide]]
   */
  def exceptionMapper[T <: Throwable](clazz: Class[_ <: ExceptionMapper[T, _]]): ThriftRouter = {
    val mapperType = superTypeFromClass(clazz, classOf[ExceptionMapper[_, _]])
    val throwableType = singleTypeParam(mapperType)
    exceptionMapper(injector.instance(clazz))(
      Manifest.classType(Class.forName(throwableType.getTypeName))
    )
    this
  }

  /**
   * Add global filter used for all requests.
   *
   * The filter is appended after other `Filters` that have already been added
   * via `filter`.
   *
   * @see The [[https://twitter.github.io/finatra/user-guide/thrift/filters.html user guide]]
   */
  def filter[FilterType <: ThriftFilter: Manifest]: ThriftRouter = {
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
  def filter[FilterType <: ThriftFilter: Manifest, Ann <: JavaAnnotation: Manifest]
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
  def filter(clazz: Class[_ <: ThriftFilter]): ThriftRouter = {
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
  def filter(filter: ThriftFilter): ThriftRouter = {
    assert(filteredThriftService == NullThriftService, "'filter' must be called before 'add'.")
    filterChain = filterChain andThen filter
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
   * Add controller used for all requests
   *
   * [[ThriftRouter]] only supports a single controller, so `add` may only be called once.
   *
   * @see the [[https://twitter.github.io/finatra/user-guide/thrift/controllers.html user guide]]
   */
  def add(controller: Controller with ToThriftService): ThriftRouter = {
    add {
      if (controller.methods.isEmpty) {
        error(s"${controller.getClass.getName} contains no visible methods. For more details see: ${ThriftRouter.url}")
      } else {
        for (m <- controller.methods) {
          m.setFilter(filterChain)
          methods += (m.method -> m)
        }
        info(
          "Adding methods\n" + controller.methods
            .map(method => s"${controller.getClass.getSimpleName}.${method.name}")
            .mkString("\n")
        )
      }
      registerMethods(methods.toMap.values.toSeq)
      registerGlobalFilter(filterChain)
      filteredThriftService = controller.toThriftService
    }
    this
  }

  /**
   * Add controller used for all requests for usage from Java
   *
   * [[ThriftRouter]] only supports a single controller, so `add` may only be called once.
   *
   * @see the [[https://twitter.github.io/finatra/user-guide/thrift/controllers.html user guide]]
   */
  def add(controller: Class[_], service: Class[_]): ThriftRouter = {
    add(
      controller,
      service,
      ThriftMux.server.params.apply[Thrift.param.ProtocolFactory].protocolFactory
    )
  }

  /**
   * Add controller used for all requests for usage from Java.
   *
   * [[ThriftRouter]] only supports a single controller, so `add` may only be called once.
   *
   * @note We do not apply filters per-method but instead all filters are applied across the service.
   *
   * @see the [[https://twitter.github.io/finatra/user-guide/thrift/controllers.html user guide]]
   */
  def add(
    controller: Class[_],
    service: Class[_],
    protocolFactory: TProtocolFactory
  ): ThriftRouter = {
    add {
      val instance = injector.instance(controller)
      val iface: Class[_] = instance.getClass.getInterfaces.head // MyService$ServiceIface
      val service: Class[_] = // MyService$Service
        // note, the $ gets concat-ed strangely to avoid a false positive scalac warning
        // for "possible missing interpolator".
        Class.forName(iface.getName.stripSuffix("$ServiceIface") + "$" + "Service")
      val constructor = service.getConstructor(iface, classOf[TProtocolFactory])
      // instantiate service
      val serviceInstance =
        constructor
          .newInstance(instance.asInstanceOf[Object], protocolFactory)
          .asInstanceOf[Service[Array[Byte], Array[Byte]]]

      val methods: Array[Method] = controller.getDeclaredMethods
      info(
        "Adding methods\n" +
          methods
            .map(
              method => s"${controller.getSimpleName}.${method.getName}"
            )
            .mkString("\n")
      )

      registerGlobalFilter(filterChain)
      registerMethods(ClassTag(service), methods.toSeq)
      filteredService = Some(
        new ThriftRequestWrapFilter[Array[Byte], Array[Byte]](controller.getSimpleName)
          .andThen(filterChain.toFilter[Array[Byte], Array[Byte]])
          .andThen(new ThriftRequestUnwrapFilter[Array[Byte], Array[Byte]])
          .andThen(serviceInstance)
      )
    }
    this
  }

  /* Private */

  private[this] def registerMethods(service: ClassTag[_], methods: Seq[Method]): Unit =
    methods.foreach(getRegistrar.register(service, _))

  private[this] def registerMethods(methods: Seq[ThriftMethodService[_, _]]): Unit =
    methods.foreach(getRegistrar.register)

  private[this] def registerGlobalFilter(filter: ThriftFilter): Unit = {
    if (filter ne ThriftFilter.Identity) {
      injector
        .instance[LibraryRegistry]
        .withSection("thrift")
        .put("filters", filter.toString)
    }
  }

  private[this] def getRegistrar: Registrar =
    new Registrar(
      injector
        .instance[LibraryRegistry]
        .withSection("thrift", "methods")
    )

  private[this] def add(f: => Unit): Unit = {
    assert(
      !done,
      "ThriftRouter#add cannot be called multiple times, as we don't currently support serving multiple thrift services."
    )
    f
    done = true
  }

  private[finatra] def serviceName(name: String): ThriftRouter = {
    this.name = name
    this
  }
}
