package com.twitter.finatra.example.exceptions;

public class HelloWorldException extends RuntimeException {

  public HelloWorldException(String message) {
    super(message);
  }
}
