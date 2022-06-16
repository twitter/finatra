package com.twitter.inject

import com.twitter.finagle.tracing.Annotation
import com.twitter.finagle.tracing.BufferingTracer
import com.twitter.finagle.tracing.Record
import com.twitter.util.jackson.ScalaObjectMapper

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets

object InMemoryTracer {

  /** Output formatter for Trace results */
  sealed trait Formatter extends (Iterator[Record] => String) {
    def apply(traceIter: Iterator[Record], outputStream: PrintStream): Unit
  }

  /** A [[Formatter]] that writes out to the console with a simple plain-text, human readable format */
  object ConsoleFormatter extends Formatter {
    def apply(traceIter: Iterator[Record]): String = {
      val baos = new ByteArrayOutputStream(512)
      val ps = new PrintStream(baos, true, StandardCharsets.UTF_8.name())
      try {
        ps.println("================= Trace Annotations =================")
        traceIter.foreach(ps.println(_))
        ps.println("=====================================================")
        baos.toString(StandardCharsets.UTF_8.name())
      } finally {
        ps.close() // also closes the underlying ByteArrayOutputStream
      }
    }

    def apply(traceIter: Iterator[Record], outputStream: PrintStream): Unit =
      outputStream.print(apply(traceIter))
  }

  /**
   *  A [[Formatter]] that writes outputs the Zipkin JSON format, which can be uploaded via the Zipkin UI to test/verify
   *  trace formatting and visualization of given Trace and its Spans.
   *
   *  @see [[https://zipkin.io/zipkin-api/zipkin2-api.yaml]]
   */
  object ZipkinJsonFormatter extends Formatter {
    private lazy val mapper = ScalaObjectMapper.builder.camelCaseObjectMapper

    /**
     * Case class representation of Zipkin Endpoint JSON
     *
     * @see [[https://zipkin.io/zipkin-api/zipkin2-api.yaml]]
     */
    private case class EndpointNode(
      serviceName: Option[String],
      ipv4: Option[String],
      ipv6: Option[String],
      port: Option[Int])

    /**
     * Case class representation of Zipkin Annotation JSON
     *
     * @see [[https://zipkin.io/zipkin-api/zipkin2-api.yaml]]
     */
    private case class AnnotationNode(timestamp: Long, value: String)

    /**
     * Case class representation of Zipkin Trace JSON
     *
     * @see [[https://zipkin.io/zipkin-api/zipkin2-api.yaml]]
     */
    private case class TraceNode(
      traceId: String,
      parentId: Option[String],
      id: String,
      kind: Option[String],
      name: Option[String],
      timestamp: Option[Long],
      duration: Option[Long],
      localEndpoint: Option[EndpointNode],
      remoteEndpoint: Option[EndpointNode],
      annotations: Seq[AnnotationNode],
      tags: Map[String, String],
      debug: Boolean)

    def apply(traceIter: Iterator[Record]): String = {
      val traceNodes = toTraceNodes(traceIter)
      mapper.writePrettyString(traceNodes)
    }

    override def apply(traceIter: Iterator[Record], outputStream: PrintStream): Unit = {
      val traceNodes = toTraceNodes(traceIter)
      mapper.writeValue(traceNodes, outputStream)
    }

    private[this] def toTraceNodes(traceIter: Iterator[Record]): Seq[TraceNode] = traceIter.toSeq
      .groupBy(_.traceId).map {
        case (traceId, records) =>
          val serviceName = records.collectFirst {
            case Record(_, _, Annotation.ServiceName(sn), _) => sn
          }

          val duration: Option[Long] = records.collectFirst {
            case Record(_, _, _, Some(duration)) => duration.inMicroseconds
          } match {
            case r @ Some(_) => r
            case _ => Some((records.last.timestamp - records.head.timestamp).inMicroseconds)
          }

          TraceNode(
            traceId = traceId.traceId.toString,
            parentId = Some(traceId.parentId.toString),
            id = traceId.spanId.toString,
            kind = records.collectFirst {
              case Record(_, _, Annotation.ServerSend, _) => "SERVER"
              case Record(_, _, Annotation.ClientSend, _) => "CLIENT"
            },
            name = records.collectFirst {
              case Record(_, _, Annotation.Rpc(name), _) => name
            },
            timestamp = records.lastOption.map(_.timestamp.inMicroseconds),
            duration = duration,
            localEndpoint = records.collectFirst {
              case Record(_, _, Annotation.LocalAddr(ia), _) =>
                EndpointNode(
                  serviceName = serviceName,
                  ipv4 = Some(ia.getHostString),
                  ipv6 = None,
                  port = Some(ia.getPort))
              case Record(_, _, Annotation.ServiceName(svc), _) =>
                // this is a secondary case for Local Tracing
                EndpointNode(
                  serviceName = Some(svc),
                  ipv4 = None,
                  ipv6 = None,
                  port = None
                )
            },
            remoteEndpoint = records.collectFirst {
              case Record(_, _, Annotation.ClientAddr(ia), _) =>
                EndpointNode(
                  serviceName = None,
                  ipv4 = Some(ia.getHostString),
                  ipv6 = None,
                  port = Some(ia.getPort))
            },
            annotations = records.collect {
              case Record(_, timestamp, Annotation.WireRecv, _) =>
                AnnotationNode(timestamp.inMicroseconds, "wr")
              case Record(_, timestamp, Annotation.WireSend, _) =>
                AnnotationNode(timestamp.inMicroseconds, "ws")
              case Record(_, timestamp, Annotation.Message(msg), _) =>
                AnnotationNode(timestamp.inMicroseconds, msg)
            },
            tags = records.collect {
              case Record(_, _, Annotation.BinaryAnnotation(k, v), _) => (k -> v.toString)
            }.toMap,
            debug = true
          )
      }.toSeq

  }

}

final class InMemoryTracer extends BufferingTracer {

  private[InMemoryTracer] class BinaryAnnotations {

    /** Get the first observed [[Annotation.BinaryAnnotation]] with the given key */
    def get(key: String): Option[Annotation.BinaryAnnotation] = iterator.collectFirst {
      case Record(_, _, ba: Annotation.BinaryAnnotation, _) if key == ba.key => ba
    }

    /** Get the first observed [[Annotation.BinaryAnnotation]] with the given key and value */
    def get(key: String, value: Any): Option[Annotation.BinaryAnnotation] = iterator.collectFirst {
      case Record(_, _, ba: Annotation.BinaryAnnotation, _) if key == ba.key && value == ba.value =>
        ba
    }

    /**
     * Return the first observed [[Annotation.BinaryAnnotation]] with the given key
     *
     * @note Throws [[IllegalArgumentException]] if the key is not present
     */
    def apply(key: String): Annotation.BinaryAnnotation = apply(key, true)

    /**
     * Return the first observed [[Annotation.BinaryAnnotation]] with the given key
     *
     * @note Throws [[IllegalArgumentException]] if the key is not present
     */
    def apply(key: String, verbose: Boolean): Annotation.BinaryAnnotation = get(key) match {
      case Some(anno) =>
        anno
      case _ =>
        if (verbose) print()
        throw new IllegalArgumentException(s"Binary Annotation with key '$key' does not exist")
    }

    /**
     * Return the first observed [[Annotation.BinaryAnnotation]] with the given key and value
     *
     * @note Throws [[IllegalArgumentException]] if the key and value are not present
     */
    def apply(key: String, value: Any): Annotation.BinaryAnnotation = apply(key, value, true)

    /**
     * Return the first observed [[Annotation.BinaryAnnotation]] with the given key and value
     *
     * @note Throws [[IllegalArgumentException]] if the key and value are not present
     */
    def apply(key: String, value: Any, verbose: Boolean): Annotation.BinaryAnnotation =
      get(key, value) match {
        case Some(anno) =>
          anno
        case _ =>
          if (verbose) print()
          throw new IllegalArgumentException(
            s"Binary Annotation with key '$key' and value '$value' does not exist")
      }

  }

  private[InMemoryTracer] class Messages {

    /** Get the first observed [[Annotation.Message]] with the given exact message */
    def get(message: String): Option[Annotation.Message] = iterator.collectFirst {
      case Record(_, _, am @ Annotation.Message(msg), _) if message == msg => am
    }

    /**
     * Return the first observed [[Annotation.Message]] with the given exact message
     *
     * @note Throws [[IllegalArgumentException]] if the message is not present
     */
    def apply(message: String): Annotation.Message = apply(message, true)

    /**
     * Return the first observed [[Annotation.Message]] with the given exact message
     *
     * @note Throws [[IllegalArgumentException]] if the message is not present
     */
    def apply(message: String, verbose: Boolean): Annotation.Message = get(message) match {
      case Some(msg) =>
        msg
      case _ =>
        if (verbose) print()
        throw new IllegalArgumentException(
          s"Message Annotation with message '$message' does not exist")
    }

  }

  private[InMemoryTracer] class ServiceNames {

    /** Get the first observed [[Annotation.ServiceName]] with the given exact service name */
    def get(serviceName: String): Option[Annotation.ServiceName] = iterator.collectFirst {
      case Record(_, _, sn: Annotation.ServiceName, _) if sn.service == serviceName => sn
    }

    /**
     * Return the first observed [[Annotation.ServiceName]] with the given exact service name
     *
     * @note Throws [[IllegalArgumentException]] if the service name is not present
     */
    def apply(serviceName: String): Annotation.ServiceName = apply(serviceName, true)

    /**
     * Return the first observed [[Annotation.ServiceName]] with the given exact service name
     *
     * @note Throws [[IllegalArgumentException]] if the service name is not present
     */
    def apply(serviceName: String, verbose: Boolean): Annotation.ServiceName = get(
      serviceName) match {
      case Some(svc) =>
        svc
      case _ =>
        if (verbose) print()
        throw new IllegalArgumentException(
          s"ServiceName Annotation with name '$serviceName' does not exist")
    }

  }

  private[InMemoryTracer] class Rpcs {

    /** Get the first observed [[Annotation.Rpc]] with the given exact name */
    def get(rpc: String): Option[Annotation.Rpc] = iterator.collectFirst {
      case Record(_, _, r @ Annotation.Rpc(name), _) if name == rpc => r
    }

    /**
     * Return the first observed [[Annotation.Rpc]] with the given exact name
     *
     * @note Throws [[IllegalArgumentException]] if the rpc name is not present
     */
    def apply(rpc: String): Annotation.Rpc = apply(rpc, true)

    /**
     * Return the first observed [[Annotation.Rpc]] with the given exact name
     *
     * @note Throws [[IllegalArgumentException]] if the rpc name is not present
     */
    def apply(rpc: String, verbose: Boolean): Annotation.Rpc = get(rpc) match {
      case Some(r) =>
        r
      case _ =>
        if (verbose) print()
        throw new IllegalArgumentException(s"RPC Annotation with identifier '$rpc' does not exist")
    }
  }

  /** Utility for introspecting [[Annotation.BinaryAnnotation]] records present in this [[InMemoryTracer]] */
  val binaryAnnotations: BinaryAnnotations = new BinaryAnnotations

  /** Utility for introspecting [[Annotation.Message]] records present in this [[InMemoryTracer]] */
  val messages: Messages = new Messages

  /** Utility for introspecting [[Annotation.ServiceName]] records present in this [[InMemoryTracer]] */
  val serviceNames: ServiceNames = new ServiceNames

  /** Utility for introspecting [[Annotation.Rpc]] records present in this [[InMemoryTracer]] */
  val rpcs: Rpcs = new Rpcs

  def print(): Unit = print(InMemoryTracer.ConsoleFormatter)
  def print(ps: PrintStream): Unit = print(InMemoryTracer.ConsoleFormatter, ps)
  def print(formatter: InMemoryTracer.Formatter): Unit = print(formatter, Console.out)
  def print(formatter: InMemoryTracer.Formatter, ps: PrintStream): Unit = formatter(iterator, ps)

}
