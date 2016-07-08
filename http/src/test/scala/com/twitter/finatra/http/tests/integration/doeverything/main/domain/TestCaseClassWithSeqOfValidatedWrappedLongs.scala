package com.twitter.finatra.http.tests.integration.doeverything.main.domain

case class TestCaseClassWithSeqOfValidatedWrappedLongs(
  seq: Seq[ValidatedWrappedLong])
