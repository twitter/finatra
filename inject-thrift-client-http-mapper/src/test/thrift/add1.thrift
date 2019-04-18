namespace java com.twitter.adder.thriftjava
#@namespace scala com.twitter.adder.thriftscala
namespace rb Adder

service Adder {

  i32 add1(
    1: i32 num
  )

  string add1String(
    1: string num
  )

  string add1Slowly(
    1: string num
  )

  string add1AlwaysError(
    1: string num
  )
}
