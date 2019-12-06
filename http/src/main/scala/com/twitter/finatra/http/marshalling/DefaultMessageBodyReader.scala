package com.twitter.finatra.http.marshalling

import com.twitter.finagle.http.Message

/**
 * Marker trait denoting a class that can act as a default [[com.twitter.finatra.http.marshalling.MessageBodyReader]]
 * for the HTTP server.
 *
 * This default is invoked when the `com.twitter.finatra.http.internal.CallbackConverter`
 * cannot find a suitable [[com.twitter.finatra.http.marshalling.MessageBodyReader]] to convert the
 * incoming Finagle request into the route callback input type. E.g., given a defined route in a Controller:
 *
 * {{{
 *   get("/") { request: T =>
 *      ...
 *   }
 * }}}
 *
 * The `com.twitter.finatra.http.internal.CallbackConverter` attempts to locate a
 * [[com.twitter.finatra.http.marshalling.MessageBodyManager]] which can parse a Finagle
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
 * The framework default [[com.twitter.finatra.http.marshalling.MessageBodyReader]] will attempt to
 * convert the incoming request body using the server's configured [[com.twitter.finatra.json.FinatraObjectMapper]]
 * and is the basis for the framework's [[https://twitter.github.io/finatra/user-guide/json/routing.html#requests JSON Integration with Routing]].
 *
 * The [[com.twitter.finatra.http.marshalling.DefaultMessageBodyReader]] trait differs from the
 * [[com.twitter.finatra.http.marshalling.MessageBodyReader]] trait in that the
 * default is not parameterized to a specific type but instead defines a generic `parse[T]` function
 * which can generate a type `T` from a given request.
 *
 * A [[com.twitter.finatra.http.marshalling.MessageBodyReader]] is expected to only
 * ever generate a single type `T` from its `parse[T]` implementation.
 *
 * @note this class is explicitly NOT an extension of [[com.twitter.finatra.http.marshalling.MessageBodyReader]]
 *       because it is bound to the object graph and needs to support injectable value lookup by
 *       type passed into the parse method.
 *
 * @see [[com.twitter.finatra.http.internal.marshalling.DefaultMessageBodyReaderImpl]]
 * @see [[com.twitter.finatra.http.marshalling.MessageBodyManager]]
 * @see [[com.twitter.finatra.http.modules.MessageBodyModule]]
 * @see [[com.twitter.finatra.http.HttpServer.messageBodyModule]]
 * @see [[https://twitter.github.io/finatra/user-guide/json/routing.html#requests]]
 */
trait DefaultMessageBodyReader {
  def parse[T: Manifest](message: Message): T
}
