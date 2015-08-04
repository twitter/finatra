package com.twitter.hello

/**
 * Represents Pets in the Petstore. Each Pet has a unique ID that should not be known by
 * the user at the time of its creation.
 * @param id The pet's auto-generated, unique ID.
 * @param name (Required) The pet's name.
 * @param photoUrls (Required) A sequence of URLs that lead to uploaded photos of the pet.
 * @param category The type of pet (cat, dragon, fish, etc.)
 * @param tags Tags that describe this pet.
 * @param status (Available, Pending, or Adopted)
 */
case class Pet(
    id: Option[Long],
    name: String,
    photoUrls: Seq[String],
    category: Option[Category],
    tags: Option[Seq[Tag]],
    status: Option[Status] //available, pending, adopted
    )

/**
 * Provides a codec for decoding and encoding Pet objects.
 */
object Pet {
}
