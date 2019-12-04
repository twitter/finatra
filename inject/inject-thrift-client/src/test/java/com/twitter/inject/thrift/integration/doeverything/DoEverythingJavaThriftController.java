package com.twitter.inject.thrift.integration.doeverything;

import javax.inject.Inject;

import com.twitter.test.thriftjava.EchoService;
import com.twitter.util.Future;

class DoEverythingJavaThriftController implements EchoService.ServiceIface {

    @Inject
    public DoEverythingJavaThriftController() { }

    @Override
    public Future<String> echo(String msg) {
        return Future.value(msg);
    }

    @Override
    public Future<Integer> setTimesToEcho(int times) {
        return Future.value(times);
    }

}
