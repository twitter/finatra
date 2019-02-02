package com.twitter.finatra.kafkastreams.integration.compositesum

import UserClicksTypes.UserId

case class UserClicks(userId: UserId, clickType: Int)
