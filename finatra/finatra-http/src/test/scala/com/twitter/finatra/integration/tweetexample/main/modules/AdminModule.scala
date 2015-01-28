package com.twitter.finatra.integration.tweetexample.main.modules

import com.google.inject.{Exposed, Provides}
import com.twitter.finatra.guice.{GuiceModule, GuicePrivateModule}
import com.twitter.finatra.integration.tweetexample.main.services.admin.{DatabaseClient, UserService}
import com.twitter.finatra.test.{Prod, Staging}
import javax.inject.Singleton

object AdminModule extends GuiceModule {

  override val modules = Seq(

    // Prod
    new GuicePrivateModule {

      @Singleton
      @Provides
      @Exposed
      @Prod
      def providesProdUserDAO(dao: UserService): UserService = {
        dao
      }

      @Singleton
      @Provides
      def providesProdDatabaseClient = {
        new DatabaseClient("data://prod")
      }
    },

    // Staging
    new GuicePrivateModule {

      @Singleton
      @Provides
      @Exposed
      @Staging
      def providesStagingUserDAO(dao: UserService): UserService = {
        dao
      }

      @Singleton
      @Provides
      def providesStagingDatabaseClient = {
        new DatabaseClient("data://staging")
      }
    })
}
