package com.twitter.finatra.kafka.serde

import com.twitter.finatra.kafka.test.thriftscala.ThriftPerson
import com.twitter.inject.Test

class AbstractThriftDelegateSerdeTest extends Test {
  test("serde") {
    val bob = Person("Bob", 22)
    val serde = new PersonSerde()

    val bobBytes = serde.serializer().serialize("topicA", bob)
    serde.deserializer().deserialize("topicA", bobBytes) should equal(bob)
  }

  case class Person(name: String, age: Int)

  class PersonSerde extends AbstractThriftDelegateSerde[Person, ThriftPerson] {
    override def toThrift(person: Person): ThriftPerson = {
      ThriftPerson(person.name, person.age.toShort)
    }

    override def fromThrift(thrift: ThriftPerson): Person = {
      Person(thrift.name, thrift.age)
    }
  }
}
