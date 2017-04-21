package com.twitter.finatra.thrift.tests.doeverything;

import javax.inject.Inject;

import com.twitter.doeverything.thriftjava.DoEverything;
import com.twitter.inject.annotations.Flag;
import com.twitter.util.Future;

class DoEverythingJavaThriftController implements DoEverything.ServiceIface {
    private final String magicNum;

    @Inject
    public DoEverythingJavaThriftController(
        @Flag("magicNum") String magicNumValue) {
        this.magicNum = magicNumValue;
    }

    @Override
    public Future<String> uppercase(String msg) {
        if ("fail".equalsIgnoreCase(msg)) {
            return Future.exception(new Exception("oops"));
        }
        return Future.value(msg.toUpperCase());
    }

    @Override
    public Future<String> echo(String msg) {
        return Future.value(msg);
    }


    @Override
    public Future<String> echo2(String msg) {
        return Future.value(msg);
    }

    @Override
    public Future<String> magicNum() {
        return Future.value(magicNum);
    }

    @Override
    public Future<String> moreThanTwentyTwoArgs(
        String one,
        String two,
        String three,
        String four,
        String five,
        String six,
        String seven,
        String eight,
        String nine,
        String ten,
        String eleven,
        String twelve,
        String thirteen,
        String fourteen,
        String fifteen,
        String sixteen,
        String seventeen,
        String eighteen,
        String nineteen,
        String twenty,
        String twentyone,
        String twentytwo,
        String twentythree) {
        return  Future.value("handled");
    }
}
