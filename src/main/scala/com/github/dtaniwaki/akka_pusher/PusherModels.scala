package com.github.dtaniwaki.akka_pusher

import spray.json.JsonFormat
import spray.json.DefaultJsonProtocol._

object PusherModels {
  case class Channel(
    occupied: Option[Boolean] = None,
    userCount: Option[Int] = None,
    subscriptionCount: Option[Int] = None
  )
  case class User(
    id: String
  )
  case class Result(
    data: String
  )
  case class AuthenticatedParams(
    auth: String,
    channelData: Option[String] = None
  )
  case class ChannelData[+T](
    /**
     * unique identifier for that user
     */
    userId: String,
    /**
     * optional set of identifying information
     */
    userInfo: Option[T] = None
  )
}
