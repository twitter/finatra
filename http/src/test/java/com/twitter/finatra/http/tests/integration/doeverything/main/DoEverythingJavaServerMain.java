package com.twitter.finatra.http.tests.integration.main;

public final class DoEverythingJavaServerMain {
    private DoEverythingJavaServerMain() {
        // Private constructor to satisfy checkstyle error:
        // "Utility classes should not have a public or default constructor)."
    }

    public static void main(String[] args) {
        new DoEverythingJavaServer().main(args);
    }
}
