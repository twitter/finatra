package com.twitter.hello.app;

import java.util.ArrayList;

final class HelloWorldAppMain {
  private HelloWorldAppMain() {
  }

  public static void main(String[] args) {
    new HelloWorldApp(new ArrayList<>()).main(args);
  }
}
