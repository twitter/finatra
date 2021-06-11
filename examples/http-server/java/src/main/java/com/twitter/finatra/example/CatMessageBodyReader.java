package com.twitter.finatra.example;

import javax.inject.Inject;

import scala.reflect.ManifestFactory;

import com.twitter.finagle.http.Message;
import com.twitter.finatra.example.domain.Cat;
import com.twitter.finatra.http.marshalling.AbstractMessageBodyReader;
import com.twitter.util.jackson.ScalaObjectMapper;

public class CatMessageBodyReader extends AbstractMessageBodyReader<Cat> {
  private final ScalaObjectMapper mapper;

  @Inject
  public CatMessageBodyReader(
      ScalaObjectMapper mapper
  ) {
    this.mapper = mapper;
  }

  @Override
  public Cat parse(Message message) {
    return mapper.parse(message.contentString(), ManifestFactory.classType(Cat.class));
  }
}
