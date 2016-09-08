package com.twitter.finatra.thrift.tests.doeverything;

final class DoEverythingJavaThriftServerMain {
    private DoEverythingJavaThriftServerMain() {
        // Private constructor to satisfy checkstyle error:
        // "Utility classes should not have a public or default constructor)."
    }

    public static void main(String[] args) {
        new DoEverythingJavaThriftServer().main(args);
    }
}
