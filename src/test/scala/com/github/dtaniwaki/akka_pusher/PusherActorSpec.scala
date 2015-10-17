package com.github.dtaniwaki.akka_pusher

import akka.actor.{ActorSystem, Props}
import akka.testkit.TestActorRef
import akka.pattern.ask
import akka.util.Timeout
import org.specs2.mutable.Specification
import org.specs2.specification.process.RandomSequentialExecution
import org.specs2.mock.Mockito
import spray.json.{JsString, JsValue, JsonWriter}
import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import scala.util.{ Failure, Success, Try }
import scala.concurrent.ExecutionContext.Implicits.global
import spray.json.DefaultJsonProtocol._

import PusherModels._
import PusherMessages._

class TestActor(_pusher: PusherClient) extends PusherActor[Map[String, String]] {
  override val pusher = _pusher
}

class PusherActorSpec extends Specification
  with SpecHelper
  with RandomSequentialExecution
  with Mockito
{
  implicit val system = ActorSystem("pusher")
  implicit val timeout = Timeout(5 seconds)
//  implicit object stringJsonFormat extends JsonWriter[String] {
//    override def write(obj: String): JsValue = JsString(obj)
//  }

  private def awaitResult[A](future: Future[A]) = Await.result(future, Duration.Inf)

  "#receive" should {
    "with TriggerMessage" in {
      "returns ResponseMessage with Result" in {
        val pusher = mock[PusherClient].smart
        pusher.trigger(anyString, anyString, anyString, any)(any) returns Future(Result(""))
        val actorRef = system.actorOf(Props(classOf[TestActor], pusher))

        val future = actorRef ? TriggerMessage("event", "channel", "message", Some("123.234"))
        awaitResult(future) === ResponseMessage(Result(""))
      }
    }
    "with ChannelMessage" in {
      "returns ResponseMessage with Channel" in {
        val pusher = mock[PusherClient].smart
        pusher.channel(anyString, any) returns Future(Channel())
        val actorRef = system.actorOf(Props(classOf[TestActor], pusher))

        val future = actorRef ? ChannelMessage("channel", Some(Seq("attr1", "attr2")))
        awaitResult(future) === ResponseMessage(Channel())
      }
    }
    "with ChannelsMessage" in {
      "returns ResponseMessage with Channels" in {
        val pusher = mock[PusherClient].smart
        pusher.channels(anyString, any) returns Future(Map[String, Channel]())
        val actorRef = system.actorOf(Props(classOf[TestActor], pusher))

        val future = actorRef ? ChannelsMessage("prefix", Some(Seq("attr1", "attr2")))
        awaitResult(future) === ResponseMessage(Map[String, Channel]())
      }
    }
    "with UsersMessage" in {
      "returns ResponseMessage with Users" in {
        val pusher = mock[PusherClient].smart
        pusher.users(anyString) returns Future(List[User]())
        val actorRef = system.actorOf(Props(classOf[TestActor], pusher))

        val future = actorRef ? UsersMessage("channel")
        awaitResult(future) === ResponseMessage(List[User]())
      }
    }
    "with AuthenticateMessage" in {
      "returns ResponseMessage with AuthenticatedParams" in {
        val actorRef = system.actorOf(PusherActor.props())
        val channelData = ChannelData(
          userId = "test_user",
          userInfo = Some(Map("foo" -> "bar"))
        )
        val future = actorRef ? AuthenticateMessage("GET", "123.234", Some(channelData))
        awaitResult(future) === ResponseMessage(AuthenticatedParams("key:5e76b03a1e16bda68b183aef8ca71fb2fad9773eae977ff3912bca2ec2d3a7e0", Some("""{"user_id":"test_user","user_info":{"foo":"bar"}}""")))
      }
      "returns ResponseMessage with AuthenticatedParams, userInfo not included" in {
        val actorRef = system.actorOf(PusherActor.props())
        val channelData = ChannelData(userId = "test_user")
        val future = actorRef ? AuthenticateMessage("GET", "123.234", Some(channelData))
        awaitResult(future) === ResponseMessage(AuthenticatedParams("key:5be264b14524c93bafdc7dbc0bdba9dd782f00a2e310bcb55ef76b26b6841f44", Some("""{"user_id":"test_user"}""")))
      }
    }
    "with ValidateSignatureMessage" in {
      "returns ResponseMessage with Boolean" in {
        val actorRef = system.actorOf(PusherActor.props())
        val future = actorRef ? ValidateSignatureMessage("key", "773ba44693c7553d6ee20f61ea5d2757a9a4f4a44d2841ae4e95b52e4cd62db4", "foo")
        awaitResult(future) === ResponseMessage(true)
      }
    }
  }
}
