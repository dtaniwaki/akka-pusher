package com.github.dtaniwaki.akka_pusher

import akka.actor.{ ActorSystem, Props }
import akka.pattern.ask
import akka.util.Timeout
import com.github.dtaniwaki.akka_pusher.attributes.{ PusherChannelAttributes, PusherChannelsAttributes }
import com.typesafe.config.ConfigFactory
import com.github.dtaniwaki.akka_pusher.PusherMessages._
import com.github.dtaniwaki.akka_pusher.PusherModels._
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.process.RandomSequentialExecution
import spray.json.DefaultJsonProtocol._
import spray.json._
import scala.collection.mutable.Queue
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }
import scala.util.Success

class TestActor(_pusher: PusherClient) extends PusherActor() {
  override val pusher = _pusher
}

class TestBatchActor(_pusher: PusherClient, _queue: Queue[TriggerMessage]) extends PusherActor(ConfigFactory.parseString("""pusher: {batchTrigger: true}""")) {
  override val pusher = _pusher
  override val batchTriggerQueue = _queue
}

class PusherActorSpec extends Specification
    with SpecHelper
    with RandomSequentialExecution
    with Mockito {
  implicit val system = ActorSystem("pusher")
  implicit val timeout = Timeout(5 seconds)

  private def awaitResult[A](future: Future[A]) = Await.result(future, Duration.Inf)

  "#receive" should {
    "with TriggerMessage" in {
      "returns Result" in {
        val pusher = mock[PusherClient].smart
        pusher.trigger(anyString, anyString, any, any)(any) returns Future(Success(Result("")))
        val actorRef = system.actorOf(Props(classOf[TestActor], pusher))

        try {
          val future = actorRef ? TriggerMessage("channel1", "event", JsString("message"), Some("123.234"))
          awaitResult(future) === Success(Result(""))
        } finally {
          system.stop(actorRef)
        }
      }
    }
    "with ChannelMessage" in {
      "returns Channel" in {
        val pusher = mock[PusherClient].smart
        pusher.channel(anyString, any) returns Future(Success(Channel()))
        val actorRef = system.actorOf(Props(classOf[TestActor], pusher))

        try {
          val future = actorRef ? ChannelMessage("channel", Seq(PusherChannelAttributes.subscriptionCount, PusherChannelAttributes.userCount))
          awaitResult(future) === Success(Channel())
        } finally {
          system.stop(actorRef)
        }
      }
    }
    "with ChannelsMessage" in {
      "returns Channels" in {
        val pusher = mock[PusherClient].smart
        pusher.channels(anyString, any) returns Future(Success(Map[String, Channel]()))
        val actorRef = system.actorOf(Props(classOf[TestActor], pusher))

        try {
          val future = actorRef ? ChannelsMessage("prefix", Seq(PusherChannelsAttributes.userCount))
          awaitResult(future) === Success(Map[String, Channel]())
        } finally {
          system.stop(actorRef)
        }
      }
    }
    "with UsersMessage" in {
      "returns Users" in {
        val pusher = mock[PusherClient].smart
        pusher.users(anyString) returns Future(Success(List[User]()))
        val actorRef = system.actorOf(Props(classOf[TestActor], pusher))

        try {
          val future = actorRef ? UsersMessage("channel")
          awaitResult(future) === Success(List[User]())
        } finally {
          system.stop(actorRef)
        }
      }
    }
    "with AuthenticateMessage" in {
      "returns AuthenticatedParams" in {
        val actorRef = system.actorOf(PusherActor.props())

        try {
          val channelData = ChannelData(
            userId = "test_user",
            userInfo = Some(Map("foo" -> "bar").toJson)
          )
          val future = actorRef ? AuthenticateMessage("GET", "123.234", Some(channelData))
          awaitResult(future) === AuthenticatedParams("key:5e76b03a1e16bda68b183aef8ca71fb2fad9773eae977ff3912bca2ec2d3a7e0", Some("""{"user_id":"test_user","user_info":{"foo":"bar"}}"""))
        } finally {
          system.stop(actorRef)
        }
      }
      "returns AuthenticatedParams, userInfo not included" in {
        val actorRef = system.actorOf(PusherActor.props())

        try {
          val channelData = ChannelData(userId = "test_user")
          val future = actorRef ? AuthenticateMessage("GET", "123.234", Some(channelData))
          awaitResult(future) === AuthenticatedParams("key:5be264b14524c93bafdc7dbc0bdba9dd782f00a2e310bcb55ef76b26b6841f44", Some("""{"user_id":"test_user"}"""))
        } finally {
          system.stop(actorRef)
        }
      }
    }
    "with ValidateSignatureMessage" in {
      "returns Boolean" in {
        val actorRef = system.actorOf(PusherActor.props())

        try {
          val future = actorRef ? ValidateSignatureMessage("key", "773ba44693c7553d6ee20f61ea5d2757a9a4f4a44d2841ae4e95b52e4cd62db4", "foo")
          awaitResult(future) === true
        } finally {
          system.stop(actorRef)
        }
      }
    }
    "with TriggerMessage if batchTrigger" in {
      "enqueue the message" in {
        val pusher = mock[PusherClient].smart
        val queue = mock[Queue[TriggerMessage]].smart
        pusher.trigger(anyString, anyString, any, any)(any) returns Future(Success(Result("")))
        queue.enqueue(any)
        val actorRef = system.actorOf(Props(classOf[TestBatchActor], pusher, queue))

        try {
          val message = TriggerMessage("channel", "event", JsString("message"), Some("123.234"))
          actorRef ! message
          Thread.sleep(0) // yield to other threads
          Thread.sleep(500)
          there was no(pusher).trigger(anyString, anyString, any, any)(any)
          there was one(queue).enqueue(message)
        } finally {
          system.stop(actorRef)
        }
      }
    }
    "with Seq[TriggerMessage] if batchTrigger" in {
      "enqueue the message" in {
        val pusher = mock[PusherClient].smart
        val queue = mock[Queue[TriggerMessage]].smart
        pusher.trigger(anyString, anyString, any, any)(any) returns Future(Success(Result("")))
        queue.enqueue(any)
        val actorRef = system.actorOf(Props(classOf[TestBatchActor], pusher, queue))

        try {
          val message1 = TriggerMessage("channel1", "event1", JsString("message1"), Some("123.234"))
          val message2 = TriggerMessage("channel2", "event2", JsString("message2"), Some("123.234"))
          actorRef ! Seq(message1, message2)
          Thread.sleep(0) // yield to other threads
          Thread.sleep(500)
          there was no(pusher).trigger(anyString, anyString, any, any)(any)
          there was one(queue).enqueue(message1)
          there was one(queue).enqueue(message2)
        } finally {
          system.stop(actorRef)
        }
      }
    }
    "with BatchTriggerTick" in {
      "handle the batch trigger" in {
        val pusher = mock[PusherClient].smart
        val queue = Queue[TriggerMessage]()
        pusher.trigger(any)(any) returns Future(Success(Result("")))
        val messages = Seq(
          TriggerMessage("channel1", "event1", JsString("message1"), Some("123.234")),
          TriggerMessage("channel1", "event2", JsString("message2"), Some("234.345"))
        )
        messages.foreach { message => queue.enqueue(message) }
        val actorRef = system.actorOf(Props(classOf[TestBatchActor], pusher, queue))

        try {
          actorRef ! BatchTriggerTick()
          Thread.sleep(0) // yield to other threads
          Thread.sleep(500)
          there was one(pusher).trigger(===(messages.map(TriggerMessage.unapply(_).get)))(any)
          queue.dequeueAll(_ => true).length === 0
        } finally {
          system.stop(actorRef)
        }
      }
      "with more than 100 messages" in {
        "handle the batch trigger in batches of 100 messages" in {
          val pusher = mock[PusherClient].smart
          val queue = Queue[TriggerMessage]()
          pusher.trigger(any)(any) returns Future(Success(Result("")))
          val messages = for (n <- 0 to 102) yield TriggerMessage(s"channel${n}", s"event${n}", JsString(s"message${n}"), Some("123.234"))
          messages foreach { message => queue.enqueue(message) }
          val actorRef = system.actorOf(Props(classOf[TestBatchActor], pusher, queue))

          try {
            actorRef ! BatchTriggerTick()
            Thread.sleep(0) // yield to other threads
            Thread.sleep(500)
            there was one(pusher).trigger(===(messages.slice(0, 100).map(TriggerMessage.unapply(_).get)))(any)
            there was one(pusher).trigger(===(messages.slice(100, 103).map(TriggerMessage.unapply(_).get)))(any)
            queue.dequeueAll(_ => true).length === 0
          } finally {
            system.stop(actorRef)
          }
        }
      }
    }
    "scheduler" should {
      "send BatchTriggerTick periodically" in {
        val pusher = mock[PusherClient].smart
        val queue = mock[Queue[TriggerMessage]].smart
        pusher.trigger(any)(any) returns Future(Success(Result("")))
        queue.dequeueAll(any) returns scala.collection.mutable.Seq[TriggerMessage]()
        val actorRef = system.actorOf(Props(classOf[TestBatchActor], pusher, queue))

        try {
          Thread.sleep(0) // yield to other threads
          Thread.sleep(2500)
          there was atLeastTwo(queue).dequeueAll(anyFunction1)
        } finally {
          system.stop(actorRef)
        }
      }
    }
  }
}
