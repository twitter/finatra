package com.twitter.finatra.thrift.tests.doeverything;

import javax.inject.Singleton;

import com.twitter.doeverything.thriftjava.Answer;
import com.twitter.doeverything.thriftjava.DoEverythingException;
import com.twitter.finatra.thrift.exceptions.ExceptionMapper;
import com.twitter.util.Future;

@Singleton
public class DoEverythingJavaExceptionMapper
    extends ExceptionMapper<DoEverythingException, Answer> {
  @Override
  public Future<Answer> handleException(DoEverythingException throwable) {
    return Future.value(new Answer("DoEverythingException caught"));
  }
}
