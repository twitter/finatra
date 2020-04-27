package com.twitter.finatra.example;

public final class HelloWorldServerMain {
  private HelloWorldServerMain() {
  }

  public static void main(String[] args) {
    new HelloWorldServer().main(args);
  }
}
