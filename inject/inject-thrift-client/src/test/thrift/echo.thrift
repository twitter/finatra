namespace java com.twitter.test.thriftjava
#@namespace scala com.twitter.test.thriftscala
namespace rb EchoService

service EchoService {

  /**
   * Echo service
   */
  string echo(
    1: string msg
  )

  /**
   * Set message
   */
  i32 setTimesToEcho(
    1: i32 times
  )
}
