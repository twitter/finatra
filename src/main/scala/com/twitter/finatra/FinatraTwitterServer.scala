package com.twitter.finatra

import com.twitter.app.App
import com.twitter.server.{Stats, Lifecycle, Admin}

//Customized TwitterServer Trait
trait FinatraTwitterServer extends App
  with Lifecycle
  with Stats
  with Logging

