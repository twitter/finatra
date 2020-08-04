package com.twitter.finatra.modules

import com.google.inject.Provides
import com.twitter.finatra.utils.Credentials
import com.twitter.inject.TwitterModule
import com.twitter.inject.annotations.Flag
import com.twitter.util.security
import java.io.File
import javax.inject.Singleton

object CredentialsFlags {

  /**
   * The location of the text file that represents the credentials to be loaded.
   * When no path is specified an "empty" com.twitter.finatra.utils.Credentials
   * will be provided.
   * @see com.twitter.util.security.Credentials
   * @see com.twitter.finatra.utils.Credentials#isEmpty
   */
  final val FilePath = "credentials.file.path"
}

object CredentialsModule extends TwitterModule {
  flag(CredentialsFlags.FilePath, "", "Path to a text file that contains credentials.")

  @Singleton
  @Provides
  def providesCredentials(@Flag(CredentialsFlags.FilePath) filePath: String): Credentials = {
    val credentialsMap =
      filePath match {
        case path if path.isEmpty =>
          Map.empty[String, String]
        case path =>
          security.Credentials(new File(path))
      }
    Credentials(credentialsMap)
  }

  /**  Java-friendly way to access this module as a singleton instance */
  def get(): this.type = this
}
