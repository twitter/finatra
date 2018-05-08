package com.twitter.finatra.http.tests.integration.mdc.main

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.inject.logging.FinagleMDCAdapter
import javax.inject.{Inject, Singleton}
import org.slf4j.MDC
import scala.collection.JavaConverters._

@Singleton
class LoggingMDCTestController @Inject()(
  userService: UserService,
  otherService: OtherService,
  anotherService: AnotherService,
  eventService: EventService
) extends Controller {

  private[this] var storedMDC: Option[Map[String, String]] = None

  put("/putUser/:id") { request: Request =>
    Option(MDC.get("traceId")) match {
      case Some(traceId) =>
        info(s"TraceId: $traceId")
      case _ => // do nothing
    }

    val id = request.params("id")
    userService.putUser(User(id))
    response.created.location(s"/user/$id")
  }

  get("/user/:id") { request: Request =>
    val id = request.params("id")
    userService.getUserById(id)
  }

  post("/sendUserNotification/:id") { request: Request =>
    val id = request.params("id")

    for {
      user <- userService.getUserById(id)
      _ = MDC.put("userId", user.id)
      _ = info(s"${MDC.get("userId")} looked up user: ${user.id}")
      userPreferences <- otherService.getUserPreferences(user)
      notificationEvent <- anotherService.sendNotification(userPreferences)
      _ = info(s"${MDC.get("userId")} sending event")
      _ <- eventService.registerEvent(notificationEvent)
    } yield {
      storeForTesting()
      user
    }
  }

  def getStoredMDC: Option[Map[String, String]] = this.storedMDC

  private def storeForTesting(): Unit = {
    this.storedMDC = Some(
      MDC.getMDCAdapter
        .asInstanceOf[FinagleMDCAdapter]
        .getPropertyContextMap
        .asScala
        .toMap
    )
  }
}
