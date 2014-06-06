package com.twitter.finatra

import java.util.UUID
import com.twitter.finagle.http.Cookie
import scala.collection.mutable

trait SessionEnabled {
  def session(implicit req: Request): Session = {
    val sessionCookie: SessionCookie = req.cookies.get("_session_id") match {
      case None    => SessionCookie()
      case Some(c) => SessionCookie(c.value)
    }

    req.response.addCookie(sessionCookie)
    SessionHolder.getOrCreateSession(sessionCookie)
  }
}

object SessionCookie {
  def apply() = new SessionCookie(newSessionId)

  def apply(value: String) = new SessionCookie(value)

  private def newSessionId = UUID.randomUUID.toString
}

case class SessionCookie(cookieValue: String, isHttpOnly: Boolean = true, secure: Boolean = false)
  extends Cookie("_session_id", cookieValue) {

  override def httpOnly = isHttpOnly

  override def isSecure = secure
}

protected class Session(val sessionId: String) {
  private val values = mutable.Map[String, String]()

  def get(key: String): Option[String] = values.get(key)

  def getOrElse(key: String, default: String): String = values.getOrElse(key, default)

  def put(key: String, value: String) {
    values(key) = value
  }
}

object SessionHolder {
  private val sessions = mutable.Map[SessionCookie, Session]()

  private def createAndAddSession(c: SessionCookie) = {
    val context = new Session(c.value)
    sessions(c) = context
    context
  }

  def getOrCreateSession(c: SessionCookie) = sessions.getOrElse(c, createAndAddSession(c))
}