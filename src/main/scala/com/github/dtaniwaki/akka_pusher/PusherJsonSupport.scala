package com.github.dtaniwaki.akka_pusher

import spray.json._
import com.github.nscala_time.time.Imports._
import org.joda.time.format._

trait PusherJsonSupport extends DefaultJsonProtocol {
  import PusherModels._

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
