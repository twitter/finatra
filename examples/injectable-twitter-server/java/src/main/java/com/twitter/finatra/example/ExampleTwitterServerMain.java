package com.twitter.finatra.example;

public final class ExampleTwitterServerMain {

  private ExampleTwitterServerMain() { }

  public static void main(String[] args) {
    new ExampleTwitterServer().main(args);
  }

}
