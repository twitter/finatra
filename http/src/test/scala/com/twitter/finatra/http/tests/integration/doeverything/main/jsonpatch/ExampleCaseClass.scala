package com.twitter.finatra.http.tests.integration.doeverything.main.jsonpatch

case class Level0CaseClass(level0: Level1CaseClass)
case class Level1CaseClass(level1: Level2CaseClass)
case class Level2CaseClass(level2: ExampleCaseClass)
case class ExampleCaseClass(hello: String)
case class RootCaseClass(root: DuoCaseClass)
case class DuoCaseClass(left: DuoStringCaseClass, right: DuoStringCaseClass)
case class DuoStringCaseClass(left: String, right: String)
case class InnerSeqCaseClass(listOfBears: Seq[String])
