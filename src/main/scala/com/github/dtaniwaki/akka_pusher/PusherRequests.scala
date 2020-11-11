package com.github.dtaniwaki.akka_pusher

import com.github.nscala_time.time.Imports._
import PusherEvents._

object PusherRequests {
  case class AuthRequest(
    socketId: String,
    channelName: String)

  case class WebhookRequest(
    timeMs: DateTime,
    events: Seq[PusherEvent])

}
