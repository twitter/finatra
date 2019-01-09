package com.twitter.inject

import com.google.inject.Module
import com.twitter.finatra.kafkastreams.KafkaStreamsTwitterServer
import scala.collection.mutable

//TODO: DINS-2387: Update com.twitter.inject.app.App to avoid the need for reflection below
object AppAccessor {
  private val appClass = classOf[com.twitter.inject.app.App]

  /* Public */

  def callInits(server: KafkaStreamsTwitterServer): Unit = {
    val initsMethod = classOf[com.twitter.app.App].getMethods.find(_.getName.endsWith("inits")).get
    initsMethod.setAccessible(true)
    val inits = initsMethod.invoke(server).asInstanceOf[mutable.Buffer[() => Unit]]
    for (f <- inits) {
      f()
    }
  }

  def callParseArgs(server: KafkaStreamsTwitterServer, args: Map[String, String]): Unit = {
    val parseArgsMethod = appClass.getMethod("parseArgs", classOf[Array[String]])
    parseArgsMethod.invoke(server, flagsAsArgs(args))
  }

  def callAddFrameworkOverrideModules(
    server: KafkaStreamsTwitterServer,
    overrideModules: Seq[Module]
  ): Unit = {
    server.addFrameworkOverrideModules(overrideModules: _*)
  }

  def loadAndSetInstalledModules(server: KafkaStreamsTwitterServer): Injector = {
    val installedModules = server.loadModules()
    val injector = installedModules.injector

    val setInstalledModulesMethod = appClass.getMethods
      .find(_.toString.contains("installedModules_$eq")).get

    setInstalledModulesMethod.invoke(server, installedModules)

    injector
  }

  def flagsAsArgs(flags: Map[String, String]): Array[String] = {
    flags.map { case (k, v) => "-" + k + "=" + v }.toArray
  }
}
