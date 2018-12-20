package com.twitter.finatra.kafka.serde

import com.twitter.inject.Test

class AbstractSerdeTest extends Test {

  private val bob = Person("Bob", 22)
  private val serde = new PersonSerde()

  test("serde") {
    val bobBytes = serde.serializer().serialize("topicA", bob)
    serde.deserializer().deserialize("topicA", bobBytes) should equal(bob)

    val reusablePerson = Person("", 0)
    serde.deserialize(bobBytes, reusablePerson)
    reusablePerson should equal(bob)
  }

  private case class Person(var name: String, var age: Int)

  private class PersonSerde extends AbstractSerde[Person] with ReusableDeserialize[Person] {

    override def deserialize(bytes: Array[Byte]): Person = {
      val personParts = new String(bytes).split(',')
      Person(personParts(0), personParts(1).toInt)
    }

    override def deserialize(bytes: Array[Byte], reusable: Person): Unit = {
      val personParts = new String(bytes).split(',')
      reusable.name = personParts(0)
      reusable.age = personParts(1).toInt
    }

    override def serialize(person: Person): Array[Byte] = {
      s"${person.name},${person.age}".getBytes
    }
  }
}
