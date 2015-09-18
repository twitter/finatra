package com.twitter.hello.server;

final class HelloWorldServerMain {
    private HelloWorldServerMain() {
    }

    public static void main(String[] args) {
        new HelloWorldServer().main(args);
    }
}
