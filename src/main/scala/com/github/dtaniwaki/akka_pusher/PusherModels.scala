package com.github.dtaniwaki.akka_pusher

object PusherModels {
  case class Channels(
    data: String
  )
  case class Channel(
    data: String
  )
  case class Users(
    data: String
  )
  case class Result(
    data: String
  )
  case class AuthenticatedParams(
    auth: String,
    channelData: Option[String] = None
  )
  case class ChannelData(
    /**
     * unique identifier for that user
     */
    userId: String,
    /**
     * optional set of identifying information
     */
    userInfo: Option[Map[String, String]] = None
  )
}
