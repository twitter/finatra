package com.twitter.http.tests.integration.jsonpatch

import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.http.jsonpatch._
import com.twitter.inject.Test
import com.fasterxml.jackson.databind.JsonNode

/** This will test the json patch implementation vs. the json patch test suite from https://github.com/json-patch/json-patch-tests
    A possible improvement is to add that git repo as a submodule or simply add the files as resource files,
    but that is not my decision to make.

    I've inlined the test files because that's the easiest way for me to create the tests (see JsonPatchTestSuite)
  */
class JsonPatchTest extends Test {
  val finatraObjectMapper: FinatraObjectMapper = FinatraObjectMapper.create()
  val jsonPatchOperator: JsonPatchOperator = new JsonPatchOperator(finatraObjectMapper)

  // prefix and index to be sure to have no colliding test names
  def runTestCases(prefix: String, testCases: Seq[JsonPatchTestCase]): Unit = {
    testCases.zipWithIndex.foreach {
      case (JsonPatchTestCase(comment, error, doc, patch, expected), index) =>
        test(s"""$prefix[$index]: ${comment.orElse(error).getOrElse("Unnamed test")}""") {

          error match {
            // an error is expected
            case Some(err) =>
              val interceptedException = intercept[JsonPatchException] {
                val result: JsonNode = JsonPatchUtility.operate(patch, jsonPatchOperator, doc)
              }
              succeed

            // an error is not expected
            case None =>
              val result: JsonNode = JsonPatchUtility.operate(patch, jsonPatchOperator, doc)
              expected match {
                // a specific result is expected
                case Some(exp) =>
                  if (result != exp) {
                    // pretty print errors so they make sense
                    fail(s"""|Expected result: ${finatraObjectMapper.writePrettyString(exp)}
                             |Actual result: ${finatraObjectMapper.writePrettyString(result)}""".stripMargin)
                  } else {
                    succeed
                  }

                  // a specific result is not expected
                case None =>
                  succeed
              }
          }
        }
    }
  }

  val specTestsJson: Seq[JsonPatchTestCase] = finatraObjectMapper.parse[Seq[JsonPatchTestCase]](JsonPatchTestSuite.specTests)
  val testsJson: Seq[JsonPatchTestCase] = finatraObjectMapper.parse[Seq[JsonPatchTestCase]](JsonPatchTestSuite.tests)

  runTestCases("spec_tests.json", specTestsJson)
  runTestCases("tests.json", testsJson)

}
