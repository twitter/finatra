package com.twitter.finatra.modules

import com.google.inject.Provides
import com.twitter.finatra.utils.Credentials
import com.twitter.inject.TwitterModule
import com.twitter.util.{Credentials => UtilCredentials}
import java.io.File
import javax.inject.Singleton

object CredentialsModule extends TwitterModule {

  /**
   * The location of the text file that represents the credentials to be loaded.
   * When no path is specified an "empty" com.twitter.finatra.utils.Credentials
   * will be provided.
   * @see com.twitter.util.Credentials
   * @see com.twitter.finatra.utils.Credentials#isEmpty
   */
  val credentialsFilePath = flag(
    "credentials.file.path",
    "",
    "Path to a text file that contains credentials.")

  @Singleton
  @Provides
  def providesCredentials: Credentials = {
    val credentialsMap =
      credentialsFilePath() match {
        case path if path.isEmpty =>
          Map.empty[String, String]
        case path =>
          UtilCredentials(new File(path))
      }
    Credentials(credentialsMap)
  }
}
