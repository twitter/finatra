namespace java com.twitter.serviceB.thriftjava
#@namespace scala com.twitter.serviceB.thriftscala
namespace rb ServiceB

include "serviceA.thrift"

service ServiceB extends serviceA.ServiceA {
    string ping()
}