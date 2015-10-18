package com.github.dtaniwaki.akka_pusher

import spray.json.JsonFormat
import spray.json.DefaultJsonProtocol._

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
  case class ChannelData[+T : JsonFormat](
    /**
     * unique identifier for that user
     */
    userId: String,
    /**
     * optional set of identifying information
     */
    userInfo: Option[T] = None
  )
  object ChannelData {
    def apply(userId: String) = new ChannelData[Map[String, String]](userId)
    def apply[T : JsonFormat](userId: String, userInfo: Some[T]) = new ChannelData(userId, userInfo)
  }
}
