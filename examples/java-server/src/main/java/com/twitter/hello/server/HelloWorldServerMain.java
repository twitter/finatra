package com.twitter.hello.server;

import java.util.concurrent.ConcurrentLinkedQueue;

final class HelloWorldServerMain {
    private HelloWorldServerMain() {
    }

    public static void main(String[] args) {
        new HelloWorldServer(new ConcurrentLinkedQueue<>()).main(args);
    }
}
