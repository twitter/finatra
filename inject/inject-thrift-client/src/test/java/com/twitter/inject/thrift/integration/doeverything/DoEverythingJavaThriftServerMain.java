package com.twitter.inject.thrift.integration.doeverything;

final class DoEverythingJavaThriftServerMain {
    private DoEverythingJavaThriftServerMain() {
        // Private constructor to satisfy checkstyle error:
        // "Utility classes should not have a public or default constructor)."
    }

    public static void main(String[] args) {
        new DoEverythingJavaThriftServer().main(args);
    }
}
