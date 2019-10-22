package com.twitter.finatra.http.tests.integration.tweetexample.main.modules

import com.google.inject.{Exposed, Module, Provides}
import com.twitter.finatra.http.tests.integration.tweetexample.main.services.admin.{DatabaseClient, UserService}
import com.twitter.finatra.test.{Prod, Staging}
import com.twitter.inject.{TwitterModule, TwitterPrivateModule}
import javax.inject.Singleton

object AdminModule extends TwitterModule {

  override val modules: Seq[Module] = Seq(
    // Prod
    new TwitterPrivateModule {

      @Singleton
      @Provides
      @Exposed
      @Prod
      def providesProdUserDAO(dao: UserService): UserService = {
        dao
      }

      @Singleton
      @Provides
      def providesProdDatabaseClient: DatabaseClient = {
        new DatabaseClient("data://prod")
      }
    },
    // Staging
    new TwitterPrivateModule {

      @Singleton
      @Provides
      @Exposed
      @Staging
      def providesStagingUserDAO(dao: UserService): UserService = {
        dao
      }

      @Singleton
      @Provides
      def providesStagingDatabaseClient: DatabaseClient = {
        new DatabaseClient("data://staging")
      }
    }
  )
}
