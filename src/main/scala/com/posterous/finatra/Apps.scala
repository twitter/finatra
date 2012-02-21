package com.posterous.finatra

import scala.collection.mutable.ListBuffer

object Apps {

  var apps = ListBuffer[Function0[_]]()

  def add(app: FinatraApp) { apps += (() => app) }

}

