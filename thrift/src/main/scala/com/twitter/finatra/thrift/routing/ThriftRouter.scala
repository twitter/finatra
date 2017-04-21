package com.twitter.finatra.thrift.routing

import com.twitter.finagle.{Thrift, ThriftMux, Service}
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.thrift._
import com.twitter.finatra.thrift.exceptions.{ExceptionManager, ExceptionMapper}
import com.twitter.finatra.thrift.internal.{ThriftMethodService, ThriftRequestUnwrapFilter, ThriftRequestWrapFilter}
import com.twitter.finatra.thrift.internal.routing.{NullThriftService, Services}
import com.twitter.inject.{Injector, Logging}
import com.twitter.inject.TypeUtils._
import com.twitter.scrooge.{ThriftMethod, ThriftService, ToThriftService}
import java.lang.annotation.{Annotation => JavaAnnotation}
import javax.inject.{Inject, Singleton}
import org.apache.thrift.protocol.TProtocolFactory
import scala.collection.mutable.{Map => MutableMap}

@Singleton
class ThriftRouter @Inject()(
  exceptionManager: ExceptionManager,
  statsReceiver: StatsReceiver,
  injector: Injector)
  extends Logging {

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

  /** Add exception mapper used for the corresponding exceptions */
  def exceptionMapper[T <: ExceptionMapper[_, _] : Manifest]: ThriftRouter = {
    exceptionManager.add[T]
    this
  }

  /** Add exception mapper used for the corresponding exceptions */
  def exceptionMapper[T <: Throwable : Manifest](mapper: ExceptionMapper[T, _]): ThriftRouter = {
    exceptionManager.add[T](mapper)
    this
  }

  /** Add exception mapper used for the corresponding exceptions */
  def exceptionMapper[T <: Throwable](clazz: Class[_ <: ExceptionMapper[T, _]]): ThriftRouter = {
    val mapperType = superTypeFromClass(clazz, classOf[ExceptionMapper[_, _]])
    val throwableType = singleTypeParam(mapperType)
    exceptionMapper(injector.instance(clazz))(Manifest.classType(Class.forName(throwableType.getTypeName)))
    this
  }

  /** Add global filter used for all requests */
  def filter[FilterType <: ThriftFilter : Manifest]: ThriftRouter = {
    filter(injector.instance[FilterType])
  }

  /** Add global filter used for all requests annotated with Annotation Type */
  def filter[FilterType <: ThriftFilter : Manifest, Ann <: JavaAnnotation : Manifest]: ThriftRouter = {
    filter(injector.instance[FilterType, Ann])
  }

  /** Add global filter used for all requests */
  def filter(clazz: Class[_ <: ThriftFilter]): ThriftRouter = {
    filter(injector.instance(clazz))
  }

  /** Add global filter used for all requests */
  def filter(filter: ThriftFilter): ThriftRouter = {
    assert(filteredThriftService == NullThriftService, "'filter' must be called before 'add'.")
    filterChain = filterChain andThen filter
    this
  }

  /** Instantiate and add thrift controller used for all requests **/
  def add[C <: Controller with ToThriftService : Manifest]: ThriftRouter = {
    val controller = injector.instance[C]
    add(controller)
  }

  /** Add controller used for all requests **/
  def add(controller: Controller with ToThriftService): ThriftRouter = {
    add {
      for (m <- controller.methods) {
        m.setFilter(filterChain)
        methods += (m.method -> m)
      }
      info("Adding methods\n" + (controller.methods.map(method => s"${controller.getClass.getSimpleName}.${method.name}") mkString "\n"))
      if (controller.methods.isEmpty) error(s"${controller.getClass.getCanonicalName} contains no methods!")
      filteredThriftService = controller.toThriftService
    }
    this
  }

  /** Add controller used for all requests for usage from Java */
  def add(controller: Class[_], service: Class[_]): ThriftRouter = {
    add(
      controller,
      service,
      ThriftMux.server
        .params.apply[Thrift.param.ProtocolFactory]
        .protocolFactory)
  }

  /** Add controller used for all requests for usage from Java */
  def add(controller: Class[_], service: Class[_], protocolFactory: TProtocolFactory): ThriftRouter = {
    add {
      val instance = injector.instance(controller)
      val iface: Class[_] = instance.getClass.getInterfaces.head // MyService$ServiceIface
      val service: Class[_] = // MyService$Service
        Class.forName(iface.getName.stripSuffix("$ServiceIface") + "$" + "Service")
      val constructor = service.getConstructor(iface, classOf[TProtocolFactory])
      // instantiate service
      val serviceInstance =
        constructor.newInstance(
          instance.asInstanceOf[Object], protocolFactory)
          .asInstanceOf[Service[Array[Byte], Array[Byte]]]

      info("Adding methods\n" + (controller.getDeclaredMethods.map(method => s"${controller.getSimpleName}.${method.getName}") mkString "\n"))
      filteredService = Some(
        new ThriftRequestWrapFilter[Array[Byte], Array[Byte]](controller.getSimpleName)
          .andThen(filterChain.toFilter[Array[Byte], Array[Byte]])
          .andThen(new ThriftRequestUnwrapFilter[Array[Byte], Array[Byte]])
          .andThen(serviceInstance))
    }
    this
  }

  /* Private */

  private def add(f: => Unit): Unit = {
    assert(!done, "ThriftRouter#add cannot be called multiple times, as we don't currently support serving multiple thrift services.")
    f
    done = true
  }

  private[finatra] def serviceName(name: String): ThriftRouter = {
    this.name = name
    this
  }
}
