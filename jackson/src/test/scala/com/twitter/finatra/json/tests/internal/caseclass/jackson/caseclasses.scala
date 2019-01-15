package com.twitter.finatra.json.tests.internal.caseclass.jackson

import com.fasterxml.jackson.annotation.{JsonIgnore, JsonProperty}
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.NumberDeserializers.BigDecimalDeserializer
import com.twitter.finatra.request.{Header, QueryParam}
import com.twitter.finatra.response.JsonCamelCase
import com.twitter.finatra.validation.NotEmpty
import org.joda.time.DateTime

/* Note: the decoder automatically changes "_i" to "i" for de/serialization:
 * See CaseClassField#jsonNameForField */
trait Aumly { @JsonProperty("i") def _i: Int; @JsonProperty("j") def _j: String }
case class Aum(_i: Int, _j: String) extends Aumly

trait Bar {
  @JsonProperty("helloWorld") @Header("accept")
  def hello: String
}
case class FooBar(hello: String) extends Bar

trait Baz extends Bar {
  @JsonProperty("goodbyeWorld")
  def hello: String
}
case class FooBaz(hello: String) extends Baz

trait BarBaz {
  @JsonProperty("goodbye")
  def hello: String
}
case class FooBarBaz(hello: String) extends BarBaz with Bar // will end up with BarBaz @JsonProperty value as trait linearization is "right-to-left"

trait Loadable {
  @JsonProperty("url")
  def uri: String
}
abstract class Resource {
  @JsonProperty("resource")
  def uri: String
}
case class File(@JsonProperty("file") uri : String) extends Resource
case class Folder(@JsonProperty("folder") uri : String) extends Resource

abstract class LoadableResource extends Loadable {
  @JsonProperty("resource")
  override def uri: String
}
case class LoadableFile(@JsonProperty("file") uri : String) extends LoadableResource
case class LoadableFolder(@JsonProperty("folder") uri : String) extends LoadableResource

trait TestTrait {
  @JsonProperty("oldness")
  def age: Int
  @NotEmpty
  def name: String
}
@JsonCamelCase
case class TestTraitImpl(
  @JsonProperty("ageness") age: Int,// should override inherited annotation from trait
  @Header name: String, // should have two annotations, one from trait and one here
  @QueryParam dateTime: DateTime,
  @JsonProperty foo: String,
  @JsonDeserialize(contentAs = classOf[BigDecimal], using = classOf[BigDecimalDeserializer])
  double: BigDecimal,
  @JsonIgnore ignoreMe: String
) extends TestTrait

