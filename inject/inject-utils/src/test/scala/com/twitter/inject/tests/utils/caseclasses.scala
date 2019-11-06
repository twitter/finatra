package com.twitter.inject.tests.utils

import com.twitter.inject.annotations._

case class CaseClassOneTwo(@Annotation1 one: String, @Annotation2 two: String)
case class CaseClassThreeFour(@Annotation3 three: String, @Annotation4 four: String)
case class CaseClassOneTwoThreeFour(@Annotation1 one: String, @Annotation2 two: String, @Annotation3 three: String, @Annotation4 four: String)

case class WithThings(@Annotation1 @Thing("thing1") thing1: String, @Annotation2 @Thing("thing2") thing2: String)
case class WithWidgets(@Annotation3 @Widget("widget1") widget1: String, @Annotation4 @Widget("widget2") widget2: String)

