namespace java com.twitter.calculator.thriftjava
#@namespace scala com.twitter.calculator.thriftscala
namespace rb Calculator

service Calculator {

  /**
   * Increment a number
   */
  i32 increment(
    1: i32 a
  )

  /**
   * Add two numbers
   */
  i32 addNumbers(
    1: i32 a
    2: i32 b
  )

  /**
   * Add two strings
   */
  string addStrings(
    1: string a
    2: string b
  )
}
