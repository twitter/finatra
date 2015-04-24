package com.twitter.finatra.utils

object RouterGenerator extends App {
  def filterParams(num: Int): String = {
    (for (i <- 1 to num) yield {
      s"F$i <: HttpFilter : Manifest"
    }) mkString ", "
  }

  def injectors(num: Int): String = {
    (for (i <- 1 to num) yield {
      s"injector.instance[F$i]"
    }) mkString " andThen "
  }

  //main
  for (i <- 1 to 10) {
    println(
      """/** Add per-controller filters (Note: Per-controller filters only run if the paired controller has a matching route) */""")

    println(
      """def add[""" + filterParams(i) + """, C <: Controller : Manifest]: Router = add[C](""" + injectors(i) + ")")
  }
}
