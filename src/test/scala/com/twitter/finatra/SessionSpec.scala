package com.twitter.finatra

import com.twitter.finatra.test.FlatSpecHelper
import com.twitter.util.Await

class SessionEnabledApp extends Controller with SessionEnabled {

  get("/new_session") { implicit request =>
    session.put("foo", "bar")
    render.plain("wooo").toFuture
  }

  get("/existing_session") { implicit request =>
    render.plain(Await.result(session.get("foo")).getOrElse("boo")).toFuture
  }
}

class SessionSpec extends FlatSpecHelper {

  val server = new FinatraServer
  server.register(new SessionEnabledApp)

  "basic session creation" should "have _session_id set" in {
    get("/new_session")
    response.getHeader("Set-Cookie") should startWith("_session_id=")
  }

  "session retrieve" should "retrieve an existing session for a given _session_id" in {
    val session = DefaultSessionHolder.getOrCreateSession(SessionCookie("TEST_SESSION_ID"))
    session.put("foo", "bar")
    get("/existing_session", headers = Map("Cookie" -> "_session_id=TEST_SESSION_ID"))
    response.body should be("bar")
  }

  "#get" should "retrieve Some(value) or None from session" in {
    val session = new DefaultSession("TEST_SESSION_ID")
    Await.result(session.put("foo", "bar"))  // ensure to be set before retrieving

    Await.result(session.get("nothing")) should be (None)
    Await.result(session.get("foo")) should be (Some("bar"))
  }

  "#put" should "set or update values in session" in {
    val session = new DefaultSession("TEST_SESSION_ID")

    Await.result(session.put("foo", "bar"))
    Await.result(session.get("foo")) should be (Some("bar"))

    Await.result(session.put("foo", "xyz"))
    Await.result(session.get("foo")) should be (Some("xyz"))

  }
}
