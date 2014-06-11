package com.twitter.finatra

import java.util.UUID
import com.twitter.finagle.http.Cookie
import scala.collection.mutable
import com.twitter.util.Future

trait SessionEnabled {
  def session(implicit req: Request): Session = {
    val sessionCookie: SessionCookie = req.cookies.get("_session_id") match {
      case None    => SessionCookie()
      case Some(c) => SessionCookie(c.value)
    }

    req.response.addCookie(sessionCookie)
    sessionHolder.getOrCreateSession(sessionCookie)
  }

  protected def sessionHolder: SessionHolder = DefaultSessionHolder
}

trait Session {
  def get(key: String): Future[Option[String]]

  def put(key: String, value: String): Future[Unit]
}

trait SessionHolder {
  def getOrCreateSession(c: SessionCookie): Session

  def deleteSession(c: SessionCookie)
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

private class DefaultSession(val sessionId: String) extends Session {
  private val values = mutable.Map[String, String]()

  def get(key: String): Future[Option[String]] = Future.value(values.get(key))

  def put(key: String, value: String): Future[Unit] = Future.value(values(key) = value)
}

private object DefaultSessionHolder extends SessionHolder {
  private val sessions = mutable.Map[SessionCookie, Session]()

  private def createAndAddSession(c: SessionCookie) = {
    val context = new DefaultSession(c.value)
    sessions(c) = context
    context
  }

  override def deleteSession(c: SessionCookie) {
    sessions.remove(c)
  }

  override def getOrCreateSession(c: SessionCookie) = sessions.getOrElse(c, createAndAddSession(c))
}