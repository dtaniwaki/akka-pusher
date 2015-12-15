package com.github.dtaniwaki.akka_pusher

import spray.json._
import com.github.nscala_time.time.Imports._
import org.joda.time.format._

trait PusherJsonSupport extends DefaultJsonProtocol {
  import PusherModels._

  implicit object ChannelMapJsonSupport extends JsonFormat[Map[String, Channel]] {
    def write(channels: Map[String, Channel]): JsValue = {
      JsObject(channels.map {
        case (name, channel) =>
          (name, channel.toJson)
      })
    }
    def read(json: JsValue): Map[String, Channel] = {
      json.asJsObject.fields.map {
        case (channelName, channelData) =>
          (channelName, channelData.convertTo[Channel])
      }
    }
  }

  implicit object UserListJsonSupport extends JsonFormat[List[User]] {
    def write(users: List[User]): JsValue = {
      JsObject("users" -> JsArray(users.map(_.toJson).toVector))
    }
    def read(json: JsValue): List[User] = {
      json.asJsObject.getFields("users") match {
        case Seq(JsArray(users)) =>
          users.map { user =>
            user.convertTo[User]
          }.toList
      }
    }
  }
}
