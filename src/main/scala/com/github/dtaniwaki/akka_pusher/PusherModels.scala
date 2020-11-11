package com.mf.location.os.service.util.pusher

object PusherModels {

  case class PusherMsg(body: String)

  case class Channel(
    occupied: Option[Boolean] = None,
    userCount: Option[Int] = None,
    subscriptionCount: Option[Int] = None)

  case class ChannelMap(channels: Map[String, Channel])

  case class User(id: String)

  case class UserList(users: List[User])

  case class Result(data: String)

  case class AuthenticatedParams(
    auth: String,
    channelData: Option[String] = None)

  case class ChannelData[PusherMsg](
    /**
     * unique identifier for that user
     */
    userId: String,
    /**
 * optional set of identifying information
 */
    userInfo: Option[PusherMsg] = None)

}
