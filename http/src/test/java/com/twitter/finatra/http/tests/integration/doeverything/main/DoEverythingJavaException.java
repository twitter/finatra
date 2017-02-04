package com.twitter.finatra.http.tests.integration.doeverything.main;

public class DoEverythingJavaException extends RuntimeException {

    public DoEverythingJavaException() {
    }

    public DoEverythingJavaException(String message) {
        super(message);
    }

    public DoEverythingJavaException(String message, Throwable cause) {
        super(message, cause);
    }

    public DoEverythingJavaException(Throwable cause) {
        super(cause);
    }

    public DoEverythingJavaException(
        String message,
        Throwable cause,
        boolean enableSuppression,
        boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
