package com.twitter.finatra.thrift.internal.routing

import com.twitter.finatra.thrift.internal.ThriftMethodService
import com.twitter.inject.internal.LibraryRegistry
import java.lang.reflect.Method

/** Performs registration of Thrift domain entities in a LibraryRegistry */
private[thrift] class Registrar(registry: LibraryRegistry) {

  def register(clazz: Class[_], thriftMethodService: ThriftMethodService[_, _]): Unit = {
    registry.put(
      Seq(thriftMethodService.method.name, "service_name"),
      thriftMethodService.method.serviceName
    )
    registry.put(Seq(thriftMethodService.method.name, "class"), clazz.getName)
    registry.put(
      Seq(thriftMethodService.method.name, "args_codec"),
      thriftMethodService.method.argsCodec.getClass.getName
    )
    registry.put(
      Seq(thriftMethodService.method.name, "response_codec"),
      thriftMethodService.method.responseCodec.getClass.getName
    )

    if (thriftMethodService.method.annotations.nonEmpty) {
      registry.put(
        Seq(thriftMethodService.method.name, "annotations"),
        thriftMethodService.method.annotations.map { case (k, v) => s"$k = $v" }.mkString(",")
      )
    }
  }

  def register(serviceName: String, clazz: Class[_], method: Method): Unit = {
    registry.put(Seq(method.getName, "service_name"), serviceName)
    registry.put(Seq(method.getName, "class"), clazz.getName)
    if (method.getParameterTypes.nonEmpty) {
      registry.put(
        Seq(method.getName, "args"),
        method.getParameterTypes
          .map { paramClass =>
            if (paramClass.getTypeParameters.nonEmpty) {
              paramClass.getName + paramClass.getTypeParameters
                .map(_.getName).mkString("[", ",", "]")
            } else paramClass.getName
          }.mkString(",")
      )
    }
    registry.put(
      Seq(method.getName, "response"),
      if (method.getReturnType.getTypeParameters.nonEmpty) {
        method.getReturnType.getName +
          method.getReturnType.getTypeParameters
            .map(_.getBounds.head.getTypeName).mkString("[", ",", "]")
      } else method.getReturnType.getName
    )
  }

}
