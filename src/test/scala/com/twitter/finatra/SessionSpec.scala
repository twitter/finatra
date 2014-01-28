package com.twitter.finatra

import com.twitter.finatra.test.FlatSpecHelper

class SessionSpec extends FlatSpecHelper {

  class ExampleApp extends Controller with Session {
    get("/foo") { implicit request =>
      session
      render.plain("").toFuture
    }

    get("/bar") { implicit request =>
      render.plain(session.get("Name").getOrElse("none :(").toString).toFuture
    }
  }

  val server = new FinatraServer
  server.register(new ExampleApp)


  "Response" should "contain session cookie header" in {
    get("/foo")
    response.originalResponse.headers().get("Set-Cookie") should startWith("_session_id")
  }

  "Session" should "be present for a given id and return its values" in {
    val session = SessionContext("TEST_CONTEXT_ID")
    session.put("Name", "Fido")
    SessionManager.add(session)

    val headers = Map("Cookie" -> "_session_id=TEST_CONTEXT_ID")
    get("/bar", headers = headers)
    response.body should be ("Fido")
  }


  "SessionManager" should "manage session contexts" in {
    val session = SessionContext("TEST_SESSION_ID")
    SessionManager.getSession("TEST_SESSION_ID") should be(None)
    SessionManager.add(session)
    SessionManager.getSession("TEST_SESSION_ID") should be(Some(session))
    SessionManager.remove("TEST_SESSION_ID")
    SessionManager.getSession("TEST_SESSION_ID") should be(None)
  }

}
