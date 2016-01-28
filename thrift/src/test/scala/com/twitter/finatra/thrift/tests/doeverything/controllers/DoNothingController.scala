package com.twitter.finatra.thrift.tests.doeverything.controllers

import com.twitter.doeverything.thriftscala.DoNothing
import com.twitter.finatra.thrift.Controller

class DoNothingController extends Controller with DoNothing.BaseServiceIface
