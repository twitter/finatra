/*
 * Copyright (C) 2009-2015 Typesafe Inc. <https://www.typesafe.com>
 */

// Adapted from github:playframework/playframework/blob/2.3.x/framework/src/play/src/main/scala/play/api/http/MediaRange.scala,
// under Apache 2 License (https://github.com/playframework/playframework#license)

package com.twitter.finatra.http.request

import com.twitter.finatra.http.request.MediaRange.MediaRangeParser
import com.twitter.util.logging.Logging
import scala.collection.BitSet
import scala.util.parsing.combinator.Parsers
import scala.util.parsing.input.CharSequenceReader

object MediaRange extends Logging {

  /**
   * Ordering for MediaRanges, in order of highest priority to lowest priority.
   *
   * The reason it is highest to lowest instead of lowest to highest is to ensure sorting is stable, so if two media
   * ranges have the same ordering weight, they will not change order.
   *
   * Ordering rules for MediaRanges:
   *
   * First compare by qValue, default to 1.  Higher means higher priority.
   * Then compare the media type.  If they are not the same, then the least specific (ie, if one is *) has a lower
   * priority, otherwise if they have same priority.
   * Then compare the sub media type.  If they are the same, the one with the more parameters has a higher priority.
   * Otherwise the least specific has the lower priority, otherwise they have the same priority.
   */
  implicit val ordering = new Ordering[MediaRange] {

    def compareQValues(x: Option[BigDecimal], y: Option[BigDecimal]) = {
      if (x.isEmpty && y.isEmpty) 0
      else if (x.isEmpty) 1
      else if (y.isEmpty) -1
      else x.get.compare(y.get)
    }

    def compare(a: MediaRange, b: MediaRange) = {
      val qCompare = compareQValues(a.qValue, b.qValue)

      if (qCompare != 0) -qCompare
      else if (a.mediaType == b.mediaType) {
        if (a.mediaSubType == b.mediaSubType) b.parameters.size - a.parameters.size
        else if (a.mediaSubType == "*") 1
        else if (b.mediaSubType == "*") -1
        else 0
      } else if (a.mediaType == "*") 1
      else if (b.mediaType == "*") -1
      else 0
    }
  }

  /**
   * Function and extractor object for parsing media ranges.
   */
  object parseAndSort {

    def apply(mediaRanges: String): Seq[MediaRange] = {
      MediaRangeParser(new CharSequenceReader(mediaRanges)) match {
        case MediaRangeParser.Success(mrs: List[MediaRange], next) =>
          if (next.atEnd) {
            debug("Reached the last media range : '" + next.source + "'")
          }
          mrs.sorted
        case MediaRangeParser.NoSuccess(err, _) =>
          debug("Unable to parse media range header '" + mediaRanges + "': " + err)
          Nil
      }
    }
  }

  /**
   * Parser for media ranges.
   *
   * Parses based on RFC2616 section 14.1, and 3.7.
   *
   * Unfortunately the specs are quite ambiguous, leaving much to our discretion.
   */
  private[request] object MediaRangeParser extends Parsers with Logging {

    val separatorChars = "()<>@,;:\\\"/[]?={} \t"
    val separatorBitSet = BitSet(separatorChars.toCharArray.map(_.toInt): _*)

    type Elem = Char

    val any = acceptIf(_ => true)(_ => "Expected any character")
    val end = not(any)

    /*
     * RFC 2616 section 2.2
     *
     * These patterns are translated directly using the same naming
     */
    val ctl = acceptIf { c =>
      (c >= 0 && c <= 0x1f) || c == 0x7f
    }(_ => "Expected a control character")
    val char = acceptIf(_ < 0x80)(_ => "Expected an ascii character")
    val text = not(ctl) ~> any
    val separators = {
      acceptIf(c => separatorBitSet(c.toInt))(_ => "Expected one of " + separatorChars)
    }

    val token = rep1(not(separators | ctl) ~> any) ^^ charSeqToString

    def badPart(p: Char => Boolean, msg: => String) = rep1(acceptIf(p)(ignoreErrors)) ^^ {
      case chars =>
        debug(msg + ": " + charSeqToString(chars))
        None
    }

    val badParameter = badPart(c => c != ',' && c != ';', "Bad media type parameter")
    val badMediaType = badPart(c => c != ',', "Bad media type")

    def tolerant[T](p: Parser[T], bad: Parser[Option[T]]) = p.map(Some.apply) | bad

    // The spec is really vague about what a quotedPair means. We're going to assume that it's just to quote quotes,
    // which means all we have to do for the result of it is ignore the slash.
    val quotedPair = '\\' ~> char
    val qdtext = not('"') ~> text
    val quotedString = '"' ~> rep(quotedPair | qdtext) <~ '"' ^^ charSeqToString

    /*
     * RFC 2616 section 3.7
     */
    val parameter = token ~ opt('=' ~> (token | quotedString)) <~ rep(' ') ^^ {
      case name ~ value => name -> value
    }

    // Either it's a valid parameter followed immediately by the end, a comma or a semicolon, or it's a bad parameter
    val tolerantParameter = tolerant(parameter <~ guard(end | ';' | ','), badParameter)

    val parameters = rep(';' ~> rep(' ') ~> tolerantParameter <~ rep(' '))
    val mediaType: Parser[MediaType] = (token <~ '/') ~ (token <~ rep(' ')) ~ parameters ^^ {
      case mainType ~ subType ~ ps => MediaType(mainType, subType, ps.flatten)
    }

    /*
     * RFC 2616 section 14.1
     *
     * Only difference between this and the spec is it treats * as valid.
     */

    // Some clients think that '*' is a valid media range.  Spec says it isn't, but it's used widely enough that we
    // need to support it.
    val mediaRange =
      (mediaType | ('*' ~> parameters.map(ps => MediaType("*", "*", ps.flatten)))) ^^ { mediaType =>
        val (params, rest) = mediaType.parameters.span(_._1 != "q")
        val (qValueStr, acceptParams) = rest match {
          case q +: ps => (q._2, ps)
          case _ => (None, Nil)
        }
        val qValue = qValueStr.flatMap { q =>
          try {
            val qbd = BigDecimal(q)
            if (qbd > 1) {
              debug("Invalid q value: " + q)
              None
            } else {
              Some(BigDecimal(q))
            }
          } catch {
            case _: NumberFormatException =>
              debug("Invalid q value: " + q)
              None
          }
        }
        new MediaRange(mediaType.mediaType, mediaType.mediaSubType, params, qValue, acceptParams)
      }

    // Either it's a valid media range followed immediately by the end or a comma, or it's a bad media type
    val tolerantMediaRange = tolerant(mediaRange.<~[Any](guard(end | ',')), badMediaType)

    val mediaRanges = rep1sep(tolerantMediaRange, ',' ~ rep(' ')).map(_.flatten)

    def apply(in: Input): ParseResult[List[MediaRange]] = mediaRanges(in)

    def ignoreErrors(c: Char) = ""

    def charSeqToString(chars: Seq[Char]) = new String(chars.toArray)
  }
}

/**
 * A media range as defined by RFC 2616 14.1
 *
 * @param mediaType The media type
 * @param mediaSubType The media sub type
 * @param parameters The parameters
 * @param qValue The Q value
 * @param acceptExtensions The accept extensions
 */
class MediaRange(
  override val mediaType: String,
  override val mediaSubType: String,
  parameters: Seq[(String, Option[String])],
  val qValue: Option[BigDecimal],
  val acceptExtensions: Seq[(String, Option[String])])
    extends MediaType(mediaType, mediaSubType, parameters) {

  /**
   * @return true if `mimeType` matches this media type, otherwise false
   */
  def accepts(mimeType: String): Boolean =
    contentType.equalsIgnoreCase(mimeType) ||
      (mediaSubType == "*" && mediaType.equalsIgnoreCase(mimeType.takeWhile(_ != '/'))) ||
      (mediaType == "*" && mediaSubType == "*")

  lazy val contentType = mediaType + "/" + mediaSubType

  override def toString = {
    new MediaType(
      mediaType,
      mediaSubType,
      parameters ++ qValue.map(q => ("q", Some(q.toString()))).toSeq ++ acceptExtensions
    ).toString
  }
}

/**
 * A media type as defined by RFC 2616 3.7.
 *
 * @param mediaType The media type
 * @param mediaSubType The media sub type
 * @param parameters The parameters
 */
case class MediaType(
  mediaType: String,
  mediaSubType: String,
  parameters: Seq[(String, Option[String])]) {
  override def toString = {
    mediaType + "/" + mediaSubType + parameters
      .map { param =>
        "; " + param._1 + param._2
          .map { value =>
            if (MediaRangeParser.token(new CharSequenceReader(value)).next.atEnd) {
              "=" + value
            } else {
              "=\"" + value.replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "\\\\\"") + "\""
            }
          }
          .getOrElse("")
      }
      .mkString("")
  }
}
