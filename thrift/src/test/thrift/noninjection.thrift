namespace java com.twitter.noninjection.thriftjava
#@namespace scala com.twitter.noninjection.thriftscala
namespace rb Noninjection

/** Test service for ensuring we can bring a server up without injection. */
service NonInjectionService {
  string echo(
    1: string msg
  )
}
