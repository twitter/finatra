package com.twitter.finatra.thrift.tests.doeverything;

import javax.inject.Inject;

import com.twitter.doeverything.thriftjava.Answer;
import com.twitter.doeverything.thriftjava.DoEverything;
import com.twitter.doeverything.thriftjava.DoEverythingException;
import com.twitter.doeverything.thriftjava.Question;
import com.twitter.finagle.RequestException;
import com.twitter.finagle.RequestTimeoutException;
import com.twitter.finatra.thrift.tests.doeverything.exceptions.BarException;
import com.twitter.finatra.thrift.tests.doeverything.exceptions.FooException;
import com.twitter.finatra.thrift.tests.doeverything.exceptions.TestChannelException;
import com.twitter.inject.annotations.Flag;
import com.twitter.util.Duration;
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
        if ("clientError".equals(msg)) {
            return Future.exception(new Exception("client error"));
        } else {
            return Future.value(msg);
        }
    }

    @Override
    public Future<String> echo2(String msg) {
        if ("clientError".equals(msg)) {
            return Future.exception(new Exception("client error"));
        } else if ("requestException".equals(msg)) {
            return Future.exception(new RequestException());
        } else if ("timeoutException".equals(msg)) {
            return Future.exception(
                new RequestTimeoutException(
                    Duration.fromSeconds(1),
                    "timeout exception"));
        } else if ("unhandledException".equals(msg)) {
            return Future.exception(new Exception("unhandled exception"));
        } else if ("barException".equals(msg)) {
            return Future.exception(new BarException());
        } else if ("fooException".equals(msg)) {
            return Future.exception(new FooException());
        } else if ("unhandledSourcedException".equals(msg)) {
            return Future.exception(new TestChannelException());
        } else if ("unhandledThrowable".equals(msg)) {
            return Future.exception(new Throwable("unhandled throwable"));
        } else {
            return Future.value(msg);
        }
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
        return Future.value("handled");
    }

    @Override
    public Future<Answer> ask(Question question) {
        if ("fail".equals(question.getText())) {
            return Future.exception(new DoEverythingException("This is a test."));
        } else {
            return Future.value(
                new Answer("The answer to the question: `"
                    + question.getText() + "` is 42."));
        }
    }
}
