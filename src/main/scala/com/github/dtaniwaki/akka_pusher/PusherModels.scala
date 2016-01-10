package com.github.dtaniwaki.akka_pusher

import spray.json._

import scala.collection.{ IterableLike, mutable, TraversableLike }

object PusherModels extends PusherJsonSupport {
  case class Channel(
    occupied: Option[Boolean] = None,
    userCount: Option[Int] = None,
    subscriptionCount: Option[Int] = None)
  object Channel {
    implicit val channelJsonSupport = jsonFormat(Channel.apply, "occupied", "user_count", "subscription_count")
  }

  case class ChannelMap(channels: Map[String, Channel]) extends Iterable[(String, Channel)] with IterableLike[(String, Channel), ChannelMap] with Equals {
    private val delegatee = channels
    override val seq = delegatee.seq
    def this(seq: ((String, Channel))*) = this(Map(seq: _*))
    def apply(k: String): Channel = delegatee.apply(k)
    def get(k: String): Option[Channel] = delegatee.get(k)
    override val iterator: Iterator[(String, Channel)] = delegatee.iterator
    override def newBuilder: mutable.Builder[(String, Channel), ChannelMap] =
      delegatee.genericBuilder.mapResult { channels => ChannelMap(channels.toSeq: _*) }

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
    def apply(seq: (String, Channel)*): ChannelMap = new ChannelMap(seq: _*)
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

  case class UserList(users: List[User]) extends Iterable[User] with IterableLike[User, UserList] with Equals {
    private val delegatee = users
    override val seq = delegatee.seq
    def this(users: User*) = this(List(users: _*))
    def apply(n: Int): User = delegatee.apply(n)
    override val iterator: Iterator[User] = delegatee.iterator
    override def newBuilder: mutable.Builder[User, UserList] =
      delegatee.genericBuilder.mapResult { users => UserList(users: _*) }

    override def hashCode(): Int = delegatee.hashCode
    override def canEqual(that: Any): Boolean = that.isInstanceOf[UserList]
    override def equals(that: Any): Boolean = {
      that match {
        case that: UserList if that canEqual this => this.delegatee.equals(that.delegatee)
        case _                                    => false
      }
    }
  }
  object UserList {
    def apply(users: User*): UserList = new UserList(users: _*)
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
