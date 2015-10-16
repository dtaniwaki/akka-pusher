package com.github.dtaniwaki.akka_pusher

trait SpecHelper {
  val resource = System.getProperty("config.resource")
  if (resource == null)
    System.setProperty("config.resource", "akka-pusher-test.conf")
}
