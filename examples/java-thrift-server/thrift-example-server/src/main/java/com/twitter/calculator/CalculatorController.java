package com.twitter.calculator;

import com.twitter.calculator.thriftjava.Calculator;
import com.twitter.util.Future;

public class CalculatorController implements Calculator.ServiceIface {

    @Override
    public Future<Integer> increment(int a) {
        return Future.value(a + 1);
    }

    @Override
    public Future<Integer> addNumbers(int a, int b) {
        return Future.value(a + b);
    }

    @Override
    public Future<String> addStrings(String a, String b) {
        Integer total = Integer.parseInt(a) + Integer.parseInt(b);
        return Future.value(total.toString());
    }
}
