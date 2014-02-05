package com.twitter.finatra

import scala.collection.mutable
import org.jboss.netty.handler.codec.http.DefaultCookie
import com.twitter.finagle.http.Cookie
import java.util.UUID

trait Session {

  def session(implicit request: Request): SessionContext = {
    request.cookie("_session_id") match {
      case None => createNewSessionWithCookie
      case Some(c) => getSessionFromCookie(c)
    }
  }

  private def getSessionFromCookie(sessionId: String)(implicit request: Request) = {
    SessionManager.getSession(sessionId) match {
      case Some(context) => context
      case None => createNewSessionWithCookie // not found? create a new one
    }
  }

  private def createNewSessionWithCookie(implicit request: Request) = {
    val uuid = UUID.randomUUID() // TODO: generate from key string
    val context = SessionContext(uuid.toString)
    SessionManager.add(context)
    request.response.addCookie(new Cookie(new DefaultCookie("_session_id", uuid.toString)))
    context
  }

}

object SessionContext {
  def apply(sessionId: String) = new SessionContext(sessionId, new DefaultSessionStore)

  def apply(sessionId: String, store: SessionStore) = new SessionContext(sessionId, store)
}

trait SessionStore {
  def put(key: String, value: Any)

  def get(key: String): Option[Any]

  def remove(key: String)
}

class DefaultSessionStore extends SessionStore {
  private val valueMap = mutable.Map[String, Any]()

  def put(key: String, value: Any) {
    valueMap.put(key, value)
  }

  def get(key: String) = valueMap.get(key)

  def remove(key: String) {
    valueMap.remove(key)
  }
}

class SessionContext(sessionId: String, store: SessionStore) {

  def id = sessionId

  def put(key: String, value: Any) = store.put(key, value)

  def get(key: String): Option[Any] = store.get(key)

  def remove(key: String) = store.remove(key)
}


object SessionManager {

  private val sessionMap: mutable.Map[String, SessionContext] = mutable.Map()

  def add(context: SessionContext) {
    sessionMap.put(context.id, context)
  }

  def getSession(sessionId: String): Option[SessionContext] = sessionMap.get(sessionId)

  def remove(sessionId: String) {
    sessionMap.remove(sessionId)
  }
}
