package com.twitter.finatra.example;

import com.google.common.collect.ImmutableMap;

import com.twitter.finagle.http.MediaType;
import com.twitter.finatra.example.domain.Dog;
import com.twitter.finatra.http.marshalling.AbstractMessageBodyWriter;
import com.twitter.finatra.http.marshalling.WriterResponse;

public class DogMessageBodyWriter extends AbstractMessageBodyWriter<Dog> {

  @Override
  public WriterResponse write(Dog dog) {
    return WriterResponse.apply(
        MediaType.PlainTextUtf8(),
        String.format(
            "Hello! My name is %s the dog. I am the color of %s.",
            dog.getName(),
            dog.getColor()),
        ImmutableMap.<String, String>of());
  }
}
