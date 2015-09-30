namespace java com.twitter.greeter.thriftjava
#@namespace scala com.twitter.greeter.thriftscala
namespace rb Greeter

exception InvalidOperation {
  1: i32 what,
  2: string why
}

exception ByeOperation {
  1: i32 code
}

struct ByeResponse {
  1: double code;
  2: string msg;
}

service Greeter {

  /**
   * Say hi
   */
  string hi(
    1: string name
  ) throws (1:InvalidOperation invalidOperation)

  /**
   * Say bye
   */
  ByeResponse bye(
    1: string name
    2: i32 age
  ) throws (1:ByeOperation byeOperation)
}
