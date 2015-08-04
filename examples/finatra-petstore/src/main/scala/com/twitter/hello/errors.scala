package com.twitter.hello

import javax.inject.Inject

import com.twitter.finagle.{SimpleFilter, Service}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.response.ResponseBuilder
import com.twitter.util.Future

///**
// * The parent error from which most PetstoreAPI errors extend. Thrown whenever something in the api goes wrong.
// */
//sealed abstract class PetstoreError(msg: String) extends Exception(msg) {
//  def message: String
//}

/**
 * Thrown when the object given is invalid (i.e. A new User or Pet contains an ID)
 * @param message An error message
 */
case class InvalidInput(message: String) extends Exception(message)

/**
 * Thrown when the given object is missing a unique ID.
 * @param message An error message
 */
case class MissingIdentifier(message: String) extends Exception(message)

/**
* Thrown when a given Pet does not exist in the database.
* @param message An error message
*/
case class MissingPet(message: String) extends Exception(message)

/**
* Thrown when the User given does not exist in the database.
* @param message An error message
*/
case class MissingUser(message: String) extends Exception(message)

/**
 * Thrown when the given Order does not exist in the database.
 * @param message An error message
 */
case class OrderNotFound(message: String) extends Exception(message)

/**
 * Thrown when a new User has the same username as an existing User. (Usernames must be unique.)
 * @param message An error message
 */
case class RedundantUsername(message: String) extends Exception(message)