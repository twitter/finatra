package com.twitter.hello.server;

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
