package com.twitter.finatra

import com.twitter.finatra.test.FlatSpecHelper

class SessionEnabledApp extends Controller with SessionEnabled {

  get("/new_session") { implicit request =>
    session.put("foo", "bar")
    render.plain("wooo").toFuture
  }

  get("/existing_session") { implicit request =>
    render.plain(session.getOrElse("foo", "<empty>")).toFuture
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
    val session = SessionHolder.getOrCreateSession(SessionCookie("TEST_SESSION_ID"))
    session.put("foo", "bar")
    get("/existing_session", headers = Map("Cookie" -> "_session_id=TEST_SESSION_ID"))
    response.body should be("bar")
  }

  "#get" should "retrieve Some(value) or None" in {
    val session = new Session("TEST_SESSION_ID")
    session.get("nothing") should be(None)
    session.put("foo", "bar")
    session.get("foo") should be(Some("bar"))
  }

  "#put" should "set or update values in session" in {
    val session = new Session("TEST_SESSION_ID")
    session.put("foo", "bar")
    session.get("foo") should be(Some("bar"))
    session.put("foo", "xyz")
    session.get("foo") should be(Some("xyz"))
  }
}
