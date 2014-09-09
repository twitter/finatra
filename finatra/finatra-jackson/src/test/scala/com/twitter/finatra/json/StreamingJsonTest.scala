package com.twitter.finatra.json

import com.twitter.finatra.json.internal.{JsonStreamParseResult, JsonArrayNotFoundException}
import com.twitter.finatra.utils.Logging
import java.io.{InputStreamReader, Reader}
import org.junit.runner.RunWith
import org.scalatest.WordSpec
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers

@RunWith(classOf[JUnitRunner])
class StreamingJsonTest extends WordSpec with ShouldMatchers with Logging {

  val mapper = FinatraObjectMapper.create()

  "json object with array" in {
    val reader = createReader("/keyed_ids.json")
    val jsonNodeAndIterator = mapper.streamParse[Long]("entity_ids", reader)

    val keyName = jsonNodeAndIterator.preArrayJsonNode.get("key").asText()
    keyName should startWith("cars")

    val numIds = (jsonNodeAndIterator.arrayIterator map { entityId =>
      println("Parsing id: " + entityId)
    }).size

    numIds should be(20)
  }

  "json object with not found array" in {
    intercept[JsonArrayNotFoundException] {
      mapper.streamParse[Long](
        "misspelled_ids",
        createReader("/keyed_ids.json"))
    }
  }

  "newline delimited json objects" in {
    val fileReader = createReader("/newline_delimited_keyed_ids.json")
    val streamResults: Iterator[JsonStreamParseResult[Long]] =
      mapper.streamParseDelimited[Long]('\n', "entity_ids", fileReader)
    
    val numParsedIds = for (streamResult <- streamResults) yield {
      val keyName = streamResult.preArrayJsonNode.get("key").asText()
      keyName should startWith("cars")

      (streamResult.arrayIterator map { entityId =>
        println("Parsing id: " + entityId)
      }).length
    }

    numParsedIds.sum should be(60)
  }

  "array of json objects" in {
    val is = getClass.getResourceAsStream("/list_keyed_ids.json")

    val keyedIds = mapper.streamParse[KeyedIds](is)
    keyedIds.size should be(3)
  }

  private def createReader(filename: String): Reader = {
    val is = getClass.getResourceAsStream(filename)
    new InputStreamReader(is, "UTF-8")
  }
}

case class KeyedIds(
  key: String,
  entity_ids: Seq[Long])
