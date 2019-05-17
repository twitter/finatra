package com.twitter.finatra.http.tests.integration.doeverything.main;

/**
 * Note that the class, all fields, and the constructor must be public (or have getter/setter
 * methods) for the class to be BeanSerializable by Jackson.
 *
 * @see [[https://github.com/FasterXML/jackson-databind/blob/master/src/main/java/com/fasterxml/jackson/databind/ser/BeanSerializer.java]]
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
