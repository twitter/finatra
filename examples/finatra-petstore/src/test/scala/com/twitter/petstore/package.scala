package com.twitter

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}

package object petstore {
  implicit val statusArbitrary: Arbitrary[Status] =
    Arbitrary(Gen.oneOf(Available, Pending, Adopted))

  implicit val orderStatArbitrary: Arbitrary[OrderStatus] =
    Arbitrary(Gen.oneOf(Placed, Approved, Delivered))

  implicit val categoryArbitrary: Arbitrary[Category] = Arbitrary(
    for {
      id <- arbitrary[Long]
      name <- Gen.alphaStr
    } yield Category(Some(id), name)
  )

  implicit val tagArbitrary: Arbitrary[Tag] = Arbitrary(
    for {
      id <- arbitrary[Long]
      name <- Gen.alphaStr
    } yield Tag(Some(id), name)
  )

  implicit val petArbitrary: Arbitrary[Pet] = Arbitrary(
    for {
      id <- arbitrary[Option[Long]]
      name <- arbitrary[String] suchThat (s=> s != null && s.nonEmpty)
      photoUrls <- arbitrary[Seq[String]]
      category <- arbitrary[Category]
      tags <- arbitrary[Seq[Tag]]
      status <- arbitrary[Status]
    } yield Pet(id, name, photoUrls, Some(category), Some(tags), Some(status))
  )

  implicit val userArbitrary: Arbitrary[User] = Arbitrary(
    for{
      id <- arbitrary[Option[Long]]
      username <- arbitrary[String] suchThat (s => s != null && s.nonEmpty)
      firstName <- arbitrary[Option[String]]
      lastName <- arbitrary[Option[String]]
      email <- arbitrary[Option[String]]
      password <- arbitrary[String] suchThat (s => s != null && s.nonEmpty)
      phone <- arbitrary[Option[String]]
    } yield User(id, username, firstName, lastName, email, password, phone)
  )

  implicit val orderArbitrary: Arbitrary[Order] = Arbitrary(
    for{
      id <- arbitrary[Option[Long]]
      petId <- arbitrary[Option[Long]]
      quantity <- arbitrary[Option[Long]]
      shipDate <- arbitrary[Option[String]]
      status <- arbitrary[OrderStatus]
      complete <- arbitrary[Option[Boolean]]
    } yield Order(id, petId, quantity, shipDate, Some(status), complete)
  )

  implicit val inventoryArbitrary: Arbitrary[Inventory] = Arbitrary(
    for{
      available <- arbitrary[Int]
      pending <- arbitrary[Int]
      adopted <- arbitrary[Int]
    } yield Inventory(available, pending, adopted)
  )

}