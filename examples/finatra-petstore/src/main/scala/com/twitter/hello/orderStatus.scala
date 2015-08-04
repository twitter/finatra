package com.twitter.hello

/**
* Represents the status of a particular order for pets. Can be "placed," "approved," or "delivered."
*/
sealed trait OrderStatus {
  /**
   * @return The string representation of the OrderStatus.
   */
  def code: String
}

/**
* The status of an order after it has been placed.
*/
case object Placed extends OrderStatus {
  /**
   * @return The string representation of the OrderStatus: "placed."
   */
  def code: String = "placed"
}

/**
* The status of an order after it has been approved by the store.
*/
case object Approved extends OrderStatus {
  /**
   * @return The string representation of the OrderStatus: "approved."
   */
  def code: String = "approved"
}

/**
* The status of an order after it has been delivered and completed.
*/
case object Delivered extends OrderStatus {
  /**
   * @return The string representation of the OrderStatus: "delivered."
   */
  def code: String = "delivered"
}

/**
* Provides encode and decode methods for OrderStatus objects.
* If asked to decode a string other than "placed," "approved," or "delivered" the
* system will fail.
*/
object OrderStatus {
  /**
   * Coverts a given string into its corresponding OrderStatus object.
   * @return OrderStatus object corresponding to s.
   */
  def fromString(s: String): OrderStatus = s match {
    case "placed" => Placed
    case "approved" => Approved
    case "delivered" => Delivered
  }
}
