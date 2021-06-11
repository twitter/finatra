package com.twitter.finatra.http.marshalling

/**
 * A marker trait denoting a class that can act as a default [[com.twitter.finatra.http.marshalling.MessageBodyWriter]]
 * for the HTTP server.
 *
 * This default is invoked by either the `com.twitter.finatra.http.internal.CallbackConverter`
 * (by invoking the [[com.twitter.finatra.http.response.ResponseBuilder]]) or
 * [[com.twitter.finatra.http.response.ResponseBuilder#write]] directly to return a suitable
 * response for a given return type.
 *
 * The framework binds [[com.twitter.finatra.http.internal.marshalling.DefaultMessageBodyWriterImpl]]
 * as an overridable implementation via the [[com.twitter.finatra.http.modules.MessageBodyModule]].
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
 * The framework default will attempt to convert the outgoing type `T` to a JSON response using the
 * server's configured [[com.twitter.util.jackson.ScalaObjectMapper]].
 *
 * The [[com.twitter.finatra.http.marshalling.DefaultMessageBodyWriter]] trait differs from the
 * [[com.twitter.finatra.http.marshalling.MessageBodyWriter]] trait in that the
 * default is not parameterized to a specific type but instead defines a generic `write[T]` function
 * which can generate a response from a given type `T`.
 *
 * @note this class is an extension of the [[com.twitter.finatra.http.marshalling.MessageBodyWriter]]
 *       trait parameterized to the `Any` type.
 *
 * @see [[com.twitter.finatra.http.internal.marshalling.DefaultMessageBodyWriterImpl]]
 * @see [[com.twitter.finatra.http.marshalling.MessageBodyManager]]
 * @see [[com.twitter.finatra.http.modules.MessageBodyModule]]
 * @see [[com.twitter.finatra.http.HttpServer.messageBodyModule]]
 * @see [[https://twitter.github.io/finatra/user-guide/json/routing.html#responses]]
 */
trait DefaultMessageBodyWriter extends MessageBodyWriter[Any]
