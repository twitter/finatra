package com.twitter.hello.server;

import javax.inject.Inject;

import scala.reflect.ManifestFactory;

import com.twitter.finagle.http.Message;
import com.twitter.finatra.http.marshalling.AbstractMessageBodyReader;
import com.twitter.finatra.json.FinatraObjectMapper;
import com.twitter.hello.server.domain.Cat;

public class CatMessageBodyReader extends AbstractMessageBodyReader<Cat> {
  private final FinatraObjectMapper mapper;

  @Inject
  public CatMessageBodyReader(
      FinatraObjectMapper mapper
  ) {
    this.mapper = mapper;
  }

  @Override
  public Cat parse(Message message) {
    return mapper.parse(message.contentString(), ManifestFactory.classType(Cat.class));
  }
}
