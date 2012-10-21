package com.twitter.finatra

class RouteVector[A] {
  var vector = Vector[A]()

  def add(x: A) {
    vector = x +: vector
  }

}
