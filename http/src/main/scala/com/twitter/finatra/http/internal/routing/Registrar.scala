package com.twitter.finatra.http.internal.routing

import com.twitter.finagle.Filter
import com.twitter.inject.internal.LibraryRegistry

/** Performs registration of HTTP domain entities in a LibraryRegistry */
private[http] class Registrar(registry: LibraryRegistry) {

  def register(route: Route): Unit = {
    val name = if (route.name.isEmpty) route.path else route.name

    registry.put(Seq(name, "class"), route.clazz.getName)
    registry.put(Seq(name, "path"), route.path)
    registry.put(Seq(name, "constant"), route.constantRoute.toString)

    registry.put(Seq(name, "callback", "request"), route.requestClass.toString)
    registry.put(Seq(name, "callback", "response"), route.responseClass.toString)
    registry.put(Seq(name, "method"), route.method.name)

    if (route.captureNames.nonEmpty) {
      registry.put(Seq(name, "capture_names"), route.captureNames.mkString(","))
    }

    if (route.routeFilter ne Filter.identity) {
      registry.put(Seq(name, "filters"), route.routeFilter.toString)
    }

    route.index match {
      case Some(index) =>
        val adminIndexKey = Seq(name, "admin", "index")
        registry.put(
          key = adminIndexKey :+ "alias",
          value = index.alias
        )
        registry.put(
          key =  adminIndexKey :+ "group",
          value = index.group
        )
        registry.put(
          key = adminIndexKey :+ "method",
          value = index.method.name
        )
        if (index.path.isDefined) {
          registry.put(
            key =  adminIndexKey :+ "path",
            value = index.path.get
          )
        }
      case _ =>
        registry.put(Seq(name, "admin"), route.admin.toString)
    }
  }
}
