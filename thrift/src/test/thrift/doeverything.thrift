namespace java com.twitter.doeverything.thriftjava
#@namespace scala com.twitter.doeverything.thriftscala
namespace rb DoEverything

exception DoEverythingException {
    1: string message
}

struct Question {
    1: string text
}

struct Answer {
    1: string text
}

service DoEverything {

  string uppercase(
    1: string msg
  )

  string echo(
    1: string msg
  )

  string echo2(
    1: string msg
  )

  string magicNum()

  string moreThanTwentyTwoArgs(
    1: string one
    2: string two
    3: string three
    4: string four
    5: string five
    6: string six
    7: string seven
    8: string eight
    9: string nine
    10: string ten
    11: string eleven
    12: string twelve
    13: string thirteen
    14: string fourteen
    15: string fifteen
    16: string sixteen
    17: string seventeen
    18: string eighteen
    19: string nineteen
    20: string twenty
    21: string twentyone
    22: string twentytwo
    23: string twentythree
  )

  Answer ask(
    1: Question question
  ) throws (
    1: DoEverythingException doEverythingException
  )
}
