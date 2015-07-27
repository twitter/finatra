package com.twitter.petstore

import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finagle.httpx.{Request, Response}
import com.twitter.util.Future

///**
//* Tells the API how to respond when certain exceptions are thrown.
//*/
//trait ErrorHandling {
//  /**
//   * Tells the service how to handle certain types of servable errors (i.e. PetstoreError)
//   */
//  def errorHandler: PartialFunction[Throwable, Response] = {
//    case RouteNotFound(route) => NotFound(
//      Map("error" -> "route_not_found", "route" -> route).asJson
//    )
//    case NotPresent(ParamItem(p)) => BadRequest(
//      Map("error" -> "param_not_present", "param" -> p).asJson
//    )
//    case NotPresent(BodyItem) => BadRequest(
//      Map("error" -> "body_not_present").asJson
//    )
//    case NotParsed(ParamItem(p), _, _) => BadRequest(
//      Map("error" -> "param_not_parsed", "param" -> p).asJson
//    )
//    case NotParsed(BodyItem, _, _) => BadRequest(
//      Map("error" -> "body_not_parsed").asJson
//    )
//    case NotValid(ParamItem(p), rule) => BadRequest(
//      Map("error" -> "param_not_valid", "param" -> p, "rule" -> rule).asJson
//    )
//    // Domain errors
//    case error: PetstoreError => NotFound(
//      Map("error" -> error.message).asJson
//    )
//  }
//
//  /**
//   * A simple Finagle filter that handles all the exceptions, which might be thrown by
//   * a request reader of one of the REST services.
//   */
//  def handleExceptions: SimpleFilter[Request,Response] = new SimpleFilter[Request, Response] {
//    def apply(req: Request, service: Service[Request, Response]): Future[Response] =
//      service(req).handle(errorHandler)
//  }
//}
