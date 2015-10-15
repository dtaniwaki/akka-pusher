package com.github.dtaniwaki.akka_pusher

import akka.actor.ActorSystem
import akka.testkit.TestActorRef
import akka.pattern.ask
import akka.util.Timeout
import org.specs2.mutable.Specification
import org.specs2.specification.process.RandomSequentialExecution
import org.specs2.mock.Mockito
import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import scala.util.{ Failure, Success, Try }
import scala.concurrent.ExecutionContext.Implicits.global

import PusherModels._
import PusherMessages._

class PusherActorSpec extends Specification
  with RandomSequentialExecution
  with Mockito
{
  implicit val system = ActorSystem("pusher")
  implicit val timeout = Timeout(5 seconds)

  private def awaitResult[A](future: Future[A]) = Await.result(future, Duration.Inf)

  "#receive" should {
    // "with TriggerMessage" should {
    //   "returns ResponseMessage with Result" in {
    //     val actorRef = system.actorOf(PusherActor.props)
    //     val pusherStub = mock[PusherClient]
    //     pusherStub.trigger("event", "channel", "message", Some("123.234")) returns Future(Result(""))
    //
    //     val future = actorRef ? TriggerMessage("event", "channel", "message", Some("123.234"))
    //     awaitResult(future) === ResponseMessage(Result(""))
    //   }
    // }
    // "with ChannelMessage" should {
    //   "returns ResponseMessage with Channel" in {
    //     val actorRef = system.actorOf(PusherActor.props)
    //     val pusherStub = mock[PusherClient]
    //     pusherStub.channel("channel", Some(Seq("attr1", "attr2"))) returns Future(Channel(""))
    //
    //     val future = actorRef ? ChannelMessage("channel", Some(Seq("attr1", "attr2")))
    //     awaitResult(future) === ResponseMessage(Channel(""))
    //   }
    // }
    // "with ChannelsMessage" should {
    //   "returns ResponseMessage with Channels" in {
    //     val actorRef = system.actorOf(PusherActor.props)
    //     val pusherStub = mock[PusherClient]
    //     pusherStub.channels("prefix", Some(Seq("attr1", "attr2"))) returns Future(Channels(""))
    //
    //     val future = actorRef ? ChannelsMessage("prefix", Some(Seq("attr1", "attr2")))
    //     awaitResult(future) === ResponseMessage(Channels(""))
    //   }
    // }
    // "with UsersMessage" should {
    //   "returns ResponseMessage with Users" in {
    //     val actorRef = system.actorOf(PusherActor.props)
    //     val pusherStub = mock[PusherClient]
    //     pusherStub.users("channel") returns Future(Users(""))
    //
    //     val future = actorRef ? UsersMessage("channel")
    //     awaitResult(future) === ResponseMessage(Users(""))
    //   }
    // }
    "with AuthenticateMessage" should {
      "returns ResponseMessage with AuthenticatedParams" in {
        val actorRef = system.actorOf(PusherActor.props)
        val channelData = PresenceChannelData(
          userId = "test_user",
          userInfo = Some(Map("foo" -> "bar"))
        )
        val future = actorRef ? AuthenticateMessage("GET", "123.234", Some(channelData))
        awaitResult(future) === ResponseMessage(AuthenticatedParams("key:5e76b03a1e16bda68b183aef8ca71fb2fad9773eae977ff3912bca2ec2d3a7e0", Some("""{"user_id":"test_user","user_info":{"foo":"bar"}}""")))
      }
      "returns ResponseMessage with AuthenticatedParams, userInfo not included" in {
        val actorRef = system.actorOf(PusherActor.props)
        val channelData = PresenceChannelData("test_user")
        val future = actorRef ? AuthenticateMessage("GET", "123.234", Some(channelData))
        awaitResult(future) === ResponseMessage(AuthenticatedParams("key:5be264b14524c93bafdc7dbc0bdba9dd782f00a2e310bcb55ef76b26b6841f44", Some("""{"user_id":"test_user"}""")))
      }
    }
    "with ValidateSignatureMessage" should {
      "returns ResponseMessage with Boolean" in {
        val actorRef = system.actorOf(PusherActor.props)
        val future = actorRef ? ValidateSignatureMessage("key", "773ba44693c7553d6ee20f61ea5d2757a9a4f4a44d2841ae4e95b52e4cd62db4", "foo")
        awaitResult(future) === ResponseMessage(true)
      }
    }
  }
}
