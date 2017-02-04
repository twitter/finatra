package com.twitter.finatra.http.tests.integration.doeverything.main;

import javax.inject.Inject;

import com.twitter.finagle.http.Request;
import com.twitter.finagle.http.Response;
import com.twitter.finatra.http.exceptions.AbstractExceptionMapper;
import com.twitter.finatra.http.response.ResponseBuilder;

public class DoEverythingJavaExceptionMapper
    extends AbstractExceptionMapper<DoEverythingJavaException> {

    private ResponseBuilder response;

    @Inject
    public DoEverythingJavaExceptionMapper(ResponseBuilder responseBuilder) {
        this.response = responseBuilder;
    }

    @Override
    public Response toResponse(Request request, DoEverythingJavaException throwable) {
        return response.internalServerError(throwable.getMessage());
    }
}
