package com.twitter.finatra.mysql.client

/**
 * [[MysqlClient]] required configuration for a connection.
 *
 * @param databaseName The name of the database to connect to.
 * @param credentials The [[Credentials]] used for the database connection.
 */
case class Config(databaseName: String, credentials: Credentials)
