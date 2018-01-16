package com.twitter.finatra.http.marshalling

import com.twitter.finagle.http.Request

/**
 * Marker trait denoting the default [[MessageBodyReader]] for the HTTP server. This default
 * [[MessageBodyReader]] is invoked when the [[com.twitter.finatra.http.internal.marshalling.CallbackConverter]]
 * cannot find a suitable [[MessageBodyReader]] to convert the incoming Finagle request into the
 * route callback input type. E.g., given a defined route in a Controller:
 *
 * {{{
 *   get("/") { request: T =>
 *      ...
 *   }
 * }}}
 *
 * The [[com.twitter.finatra.http.internal.marshalling.CallbackConverter]] attempts to locate a
 * [[com.twitter.finatra.http.internal.marshalling.MessageBodyManager]] which can parse a Finagle
 * request into the input type `T`. If one is not found, this default is invoked.
 *
 * The framework binds [[com.twitter.finatra.http.internal.marshalling.DefaultMessageBodyReaderImpl]]
 * as an overridable implementation via the [[com.twitter.finatra.http.modules.MessageBodyModule]].
 *
 * To override this implementation, provide a customized [[com.twitter.inject.TwitterModule]] by
 * overriding [[com.twitter.finatra.http.HttpServer.messageBodyModule]], e.g.,
 *
 * {{{
 *   class MyServer extends HttpServer {
 *      ...
 *      override val messageBodyModule = MyCustomMessageBodyModule
 *   }
 * }}}
 *
 * The framework default [[MessageBodyReader]] will attempt to convert the incoming request body using
 * the server's configured [[com.twitter.finatra.json.FinatraObjectMapper]] and is the basis for
 * the framework's [[https://twitter.github.io/finatra/user-guide/json/routing.html#requests JSON Integration with Routing]].
 *
 * The [[DefaultMessageBodyReader]] trait differs from the [[MessageBodyReader]] trait in that the
 * default is not parameterized to a specific type but instead defines a generic `parse[T]` function
 * which can generate a type T from a given request. A [[MessageBodyReader]] is expected to only
 * ever generate a single type `T` from it's `parse[T]` implementation.
 *
 * @see [[com.twitter.finatra.http.internal.marshalling.DefaultMessageBodyReaderImpl]]
 * @see [[com.twitter.finatra.http.internal.marshalling.MessageBodyManager]]
 * @see [[com.twitter.finatra.http.modules.MessageBodyModule]]
 * @see [[com.twitter.finatra.http.HttpServer.messageBodyModule]]
 * @see [[https://twitter.github.io/finatra/user-guide/json/routing.html#requests]]
 */
trait DefaultMessageBodyReader {

  def parse[T: Manifest](request: Request): T
}
