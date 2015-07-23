package com.twitter.petstore

/**
* Represents the general status of a [[Pet]]. This should either be [[Available]], [[Pending]], or [[Adopted]].
*/
sealed trait Status {
  /**
   * @return The string representing the value of this status.
   */
  def code: String
}

/**
* The status of a [[Pet]] when it is available for adoption.
*/
case object Available extends Status {
  /**
   * @return The string representing the value of this status: "available"
   */
  def code: String = "available"
}

/**
* The status of a [[Pet]] when it is pending for adoption, and currently unavailable for purchase.
*/
case object Pending extends Status {
  /**
   * @return The string representing the value of this status: "pending"
   */
  def code: String = "pending"
}

/**
* The status of a [[Pet]] when it has been adopted.
*/
case object Adopted extends Status {
  /**
   * @return The string representing the value of this status: "adopted"
   */
  def code: String = "adopted"
}

object Status {
  /**
   * Maps strings to their corresponding Status objects.
   * "available" => Available, "pending" => Pending, "adopted" => Adopted
   * @return Status object corresponding to passed-in String s.
   */
  def fromString(s: String): Status = s match {
    case "available" => Available
    case "pending" => Pending
    case "adopted" => Adopted
  }
}
