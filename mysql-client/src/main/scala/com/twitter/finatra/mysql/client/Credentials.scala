package com.twitter.finatra.mysql.client

/**
 * User credentials for a [[MysqlClient]] connection.
 *
 * @param user The username credential for the [[MysqlClient]] connection.
 * @param password The password credential for the [[MysqlClient]] connection.
 *
 * @note Credentials may be considered sensitive and it may not be safe to hardcode these values.
 *       Binding and providing these values using a secure method should be preferred. Here is a
 *       hypothetical example for illustrative purposes:
 *
 *       {{{
 *         class MyCredentialsModule extends TwitterModule {
 *           @Provides
 *           @Singleton
 *           @MyUserCredentials
 *           def provideUserCredentials(
 *             @MyUserCredentials user: String,
 *             secureCredentialsProvider: SecureCredentialsProvider
 *           ): Credentials = Credentials(user, secureCredentialsProvider.getUserPassword(user))
 *         }
 *       }}}
 */
case class Credentials(user: String, password: String)
