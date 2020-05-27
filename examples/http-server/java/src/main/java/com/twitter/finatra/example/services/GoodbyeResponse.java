package com.twitter.finatra.example.services;

/**
 * Note that the class, all fields, and the constructor must be public (or have getter/setter
 * methods) for the class to be BeanSerializable by Jackson.
 *
 * @see <a href="https://github.com/FasterXML/jackson-databind/blob/master/src/main/java/com/fasterxml/jackson/databind/ser/BeanSerializer.java>BeanSerializer</a>
 */
public class GoodbyeResponse {
  public final String name;
  public final String message;
  public final Integer code;

  public GoodbyeResponse(String name, String message, Integer code) {
    this.name = name;
    this.message = message;
    this.code = code;
  }
}
