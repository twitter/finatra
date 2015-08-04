package com.twitter.hello

/**
 * Represents the current state of the Petstore and how many pets are currently of which [[Status]].
 */
case class Inventory(available: Int, pending: Int, adopted: Int)