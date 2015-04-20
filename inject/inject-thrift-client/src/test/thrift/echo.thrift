namespace java com.twitter.test.thriftjava
#@namespace scala com.twitter.test.thriftscala
namespace rb EchoService

service EchoService {

  string echo(
    1: string msg
  )
}
