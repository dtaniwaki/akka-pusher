package com.github.dtaniwaki.akka_pusher

import spray.json._
import spray.json.DefaultJsonProtocol._

object PusherModels {
  case class Channel(
    occupied: Option[Boolean] = None,
    userCount: Option[Int] = None,
    subscriptionCount: Option[Int] = None)
  object Channel {
    implicit val channelJsonSupport = jsonFormat(Channel.apply, "occupied", "user_count", "subscription_count")
  }

  type ChannelMap = Map[String, Channel]
  object ChannelMap {
    def apply(): ChannelMap = Map[String, Channel]()
    def apply(seq: (String, Channel)*): ChannelMap = Map[String, Channel](seq: _*)

    implicit object ChannelMapJsonSupport extends JsonFormat[ChannelMap] {
      def write(channels: ChannelMap): JsValue = {
        JsObject(channels.map {
          case (name, channel) =>
            (name, channel.toJson)
        })
      }
      def read(json: JsValue): ChannelMap = {
        json.asJsObject.fields.map {
          case (channelName, channelData) =>
            (channelName, channelData.convertTo[Channel])
        }
      }
    }
  }

  case class User(
    id: String)
  object User {
    implicit val userJsonSupport = jsonFormat(User.apply _, "id")
  }

  case class Result(
    data: String)
  object Result {
    implicit val resultJsonSupport = jsonFormat(Result.apply _, "data")
  }

  case class AuthenticatedParams(
    auth: String,
    channelData: Option[String] = None)
  object AuthenticatedParams {
    implicit val authenticatedParamsJsonSupport = jsonFormat(AuthenticatedParams.apply _, "auth", "channel_data")
  }

  case class ChannelData[+T](
    /**
     * unique identifier for that user
     */
    userId: String,
    /**
 * optional set of identifying information
 */
    userInfo: Option[T] = None)(implicit writer: JsonFormat[T])
  object ChannelData {
    def apply(userId: String) = new ChannelData[JsValue](userId)

    implicit def channelDataJsonFormatSupport[T](implicit writer: JsonFormat[T]): JsonFormat[ChannelData[T]] = new JsonFormat[ChannelData[T]] {
      override def write(data: ChannelData[T]): JsValue =
        data.userInfo.map { info =>
          JsObject(
            "user_id" -> JsString(data.userId),
            "user_info" -> info.toJson
          )
        }.getOrElse {
          JsObject("user_id" -> JsString(data.userId))
        }

      override def read(json: JsValue): ChannelData[T] =
        json.asJsObject.getFields("user_id", "user_info") match {
          case Seq(JsString(userId)) =>
            ChannelData[T](userId)
          case Seq(JsString(userId), JsNull) =>
            ChannelData[T](userId)
          case Seq(JsString(userId), userInfo) =>
            ChannelData(userId, Some(userInfo.convertTo[T]))
        }
    }
  }
}
