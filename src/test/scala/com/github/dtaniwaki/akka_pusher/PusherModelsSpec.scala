package com.github.dtaniwaki.akka_pusher

import com.github.dtaniwaki.akka_pusher.PusherModels._
import org.specs2.mutable.Specification
import org.specs2.specification.process.RandomSequentialExecution
import spray.json._

class PusherModelsSpec extends Specification
    with SpecHelper
    with RandomSequentialExecution
    with PusherJsonSupport {

  "Result" in {
    "resultJsonFormat" should {
      "read from json object" in {
        val result = Result(data = "foo")
        """{"data": "foo"}""".parseJson.convertTo[Result] === result
      }
      "write to json object" in {
        val result = Result(data = "foo")
        result.toJson === """{"data": "foo"}""".parseJson
      }
    }
  }
  "Channel" in {
    "ChannelJsonFormat" should {
      "with all fields" in {
        "read from json object" in {
          val channel = Channel(occupied = Some(true), userCount = Some(1), subscriptionCount = Some(2))
          """{"occupied": true, "user_count": 1, "subscription_count": 2}""".parseJson.convertTo[Channel] === channel
        }
        "write to json object" in {
          val channel = Channel(occupied = Some(true), userCount = Some(1), subscriptionCount = Some(2))
          channel.toJson === """{"occupied": true, "user_count": 1, "subscription_count": 2}""".parseJson
        }
      }
    }
  }
  "User" in {
    "userJsonFormat" should {
      "with all fields" in {
        "read from json object" in {
          val user = User("123")
          """{"id": "123"}""".parseJson.convertTo[User] === user
        }
        "write to json object" in {
          val user = User("123")
          user.toJson === """{"id": "123"}""".parseJson
        }
      }
    }
  }
  "UserList" in {
    "UserListJsonFormat" should {
      "with users" in {
        "read from json object" in {
          val users = UserList(User("123"), User("234"))
          """{"users": [{"id": "123"}, {"id": "234"}]}""".parseJson.convertTo[UserList] === users
        }
        "write to json object" in {
          val users = UserList(User("123"), User("234"))
          users.toJson === """{"users": [{"id": "123"}, {"id": "234"}]}""".parseJson
        }
      }
      "without users" in {
        "read from json object" in {
          val users = UserList()
          """{"users": []}""".parseJson.convertTo[UserList] === users
        }
        "write to json object" in {
          val users = UserList()
          users.toJson === """{"users": []}""".parseJson
        }
      }
    }
  }
  "ChannelMap" in {
    "ChannelMapJsonFormat" should {
      "with channels" in {
        "read from json object" in {
          val channels = ChannelMap(
            "channel1" -> Channel(occupied = Some(true), userCount = Some(1), subscriptionCount = Some(2)),
            "channel2" -> Channel(occupied = Some(true), userCount = Some(2), subscriptionCount = Some(3))
          )
          """{"channel1": {"occupied": true, "user_count": 1, "subscription_count": 2}, "channel2": {"occupied": true, "user_count": 2, "subscription_count": 3}}""".parseJson.convertTo[ChannelMap] === channels
        }
        "write to json object" in {
          val channels = ChannelMap(
            "channel1" -> Channel(occupied = Some(true), userCount = Some(1), subscriptionCount = Some(2)),
            "channel2" -> Channel(occupied = Some(true), userCount = Some(2), subscriptionCount = Some(3))
          )
          channels.toJson === """{"channel1": {"occupied": true, "user_count": 1, "subscription_count": 2}, "channel2": {"occupied": true, "user_count": 2, "subscription_count": 3}}""".parseJson
        }
      }
      "without channels" in {
        "read from json object" in {
          val channels = ChannelMap()
          """{}""".parseJson.convertTo[ChannelMap] === channels
        }
        "write to json object" in {
          val channels = ChannelMap()
          channels.toJson === """{}""".parseJson
        }
      }
    }
  }
  "AuthenticatedParams" in {
    "authenticatedParamsJsonSupport" should {
      "with channelData" in {
        "read from json object" in {
          val auth = AuthenticatedParams(auth = "foo", channelData = Some("bar"))
          """{"auth": "foo", "channel_data": "bar"}""".parseJson.convertTo[AuthenticatedParams] === auth
        }
        "write to json object" in {
          val auth = AuthenticatedParams(auth = "foo", channelData = Some("bar"))
          auth.toJson === """{"auth": "foo", "channel_data": "bar"}""".parseJson
        }
      }
      "without channelData" in {
        "read from json object" in {
          val auth = AuthenticatedParams(auth = "foo")
          """{"auth": "foo"}""".parseJson.convertTo[AuthenticatedParams] === auth
        }
        "write to json object" in {
          val auth = AuthenticatedParams(auth = "foo")
          auth.toJson === """{"auth": "foo"}""".parseJson
        }
      }
    }
  }
  "ChannelData" in {
    "channelDataJsonReaderSupport" should {
      "read from json object" in {
        val channelData = ChannelData("user", Some(Map("foo" -> "bar")))
        """{"user_id": "user", "user_info": {"foo": "bar"}}""".parseJson.convertTo[ChannelData[Map[String, String]]] === channelData
      }
      "with null user info" in {
        "read from json object" in {
          val channelData = ChannelData("user")
          """{"user_id": "user", "user_info": null}""".parseJson.convertTo[ChannelData[Map[String, String]]] === channelData
        }
      }
      "without user info" in {
        "read from json object" in {
          val channelData = ChannelData("user")
          """{"user_id": "user"}""".parseJson.convertTo[ChannelData[Map[String, String]]] === channelData
        }
      }
    }
  }
}
