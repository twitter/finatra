package com.twitter.calculator;

final class CalculatorServerMain {
    private CalculatorServerMain() {
        // Private constructor to satisfy checkstyle error:
        // "Utility classes should not have a public or default constructor)."
    }

    public static void main(String[] args) {
        new CalculatorServer().main(args);
    }
}
