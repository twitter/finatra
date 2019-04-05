namespace java com.twitter.converter.thriftjava
#@namespace scala com.twitter.converter.thriftscala
namespace rb Converter

service Converter {

  string uppercase(
    1: string msg
  )

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
}
