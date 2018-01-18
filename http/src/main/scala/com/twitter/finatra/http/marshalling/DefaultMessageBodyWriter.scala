package com.twitter.finatra.http.marshalling

/**
 * A marker trait denoting the default [[MessageBodyWriter]] for the HTTP server. This default
 * [[MessageBodyWriter]] is invoked by either the [[com.twitter.finatra.http.internal.marshalling.CallbackConverter]]
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
 * The framework default [[MessageBodyWriter]] will attempt to convert the outgoing type `T` to a JSON
 * response using the server's configured [[com.twitter.finatra.json.FinatraObjectMapper]].
 *
 * @see [[com.twitter.finatra.http.internal.marshalling.DefaultMessageBodyWriterImpl]]
 * @see [[com.twitter.finatra.http.internal.marshalling.MessageBodyManager]]
 * @see [[com.twitter.finatra.http.modules.MessageBodyModule]]
 * @see [[com.twitter.finatra.http.HttpServer.messageBodyModule]]
 * @see [[https://twitter.github.io/finatra/user-guide/json/routing.html#responses]]
 */
trait DefaultMessageBodyWriter extends MessageBodyWriter[Any]
