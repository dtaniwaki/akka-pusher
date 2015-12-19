package com.github.dtaniwaki.akka_pusher

import spray.json._

object PusherModels extends PusherJsonSupport {
  case class Channel(
    occupied: Option[Boolean] = None,
    userCount: Option[Int] = None,
    subscriptionCount: Option[Int] = None)
  object Channel {
    implicit val channelJsonSupport = jsonFormat(Channel.apply, "occupied", "user_count", "subscription_count")
  }

  case class ChannelMap(seq: (String, Channel)*) extends Equals {
    private val delegatee = Map[String, Channel](seq: _*)
    def apply(k: String): Channel = delegatee.apply(k)
    def get(k: String): Option[Channel] = delegatee.get(k)
    def map[B](f: ((String, Channel)) => B): Iterable[B] = delegatee.map(f)
    def foreach(f: ((String, Channel)) => Unit): Unit = delegatee.foreach(f)

    override def hashCode(): Int = delegatee.hashCode
    override def canEqual(that: Any): Boolean = that.isInstanceOf[ChannelMap]
    override def equals(that: Any): Boolean = {
      that match {
        case that: ChannelMap if that canEqual this =>
          this.delegatee.equals(that.delegatee)
        case _ =>
          false
      }
    }
  }
  object ChannelMap {
    implicit object ChannelMapJsonSupport extends JsonFormat[ChannelMap] {
      def write(channels: ChannelMap): JsValue = {
        JsObject(channels.map {
          case (name, channel) =>
            (name, channel.toJson)
        }.toSeq: _*)
      }
      def read(json: JsValue): ChannelMap = {
        ChannelMap(json.asJsObject.fields.map {
          case (channelName, channelData) =>
            (channelName, channelData.convertTo[Channel])
        }.toSeq: _*)
      }
    }
  }

  case class User(
    id: String)
  object User {
    implicit val userJsonSupport = jsonFormat(User.apply _, "id")
  }

  case class UserList(seq: User*) extends Equals {
    private val list = seq.toList
    def apply(n: Int): User = list.apply(n)
    def map[B](f: (User) => B): Iterable[B] = list.map(f)
    def foreach(f: (User) => Unit): Unit = list.foreach(f)

    override def hashCode(): Int = list.hashCode
    override def canEqual(that: Any): Boolean = that.isInstanceOf[UserList]
    override def equals(that: Any): Boolean = {
      that match {
        case that: UserList if that canEqual this => this.list.equals(that.list)
        case _                                    => false
      }
    }
  }
  object UserList {
    implicit object UserListJsonSupport extends JsonFormat[UserList] {
      def write(users: UserList): JsValue = {
        JsObject("users" -> JsArray(users.map(_.toJson).toVector))
      }
      def read(json: JsValue): UserList = {
        json.asJsObject.getFields("users") match {
          case Seq(JsArray(users)) =>
            UserList(users.map { user =>
              user.convertTo[User]
            }.toSeq: _*)
        }
      }
    }
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
  }

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
