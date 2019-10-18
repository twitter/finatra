package com.twitter.inject.tests.module

class MyServiceImpl extends MyServiceInterface {
  override def add2(int: Int): Int = {
    int + 2
  }
}
