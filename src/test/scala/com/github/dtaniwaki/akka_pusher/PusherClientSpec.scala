package com.github.dtaniwaki.akka_pusher

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import com.github.dtaniwaki.akka_pusher.PusherModels._
import com.typesafe.config.ConfigFactory
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.process.RandomSequentialExecution
import spray.json.DefaultJsonProtocol._
import spray.json.{JsString, JsValue, JsonWriter}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class PusherClientSpec extends Specification
  with RandomSequentialExecution
  with SpecHelper
  with Mockito
{
  private def awaitResult[A](future: Future[A]) = Await.result(future, Duration.Inf)
  implicit val system = ActorSystem("pusher")

  "#constructor" should {
    "accept the config by argument" in {
      val pusher = new PusherClient(ConfigFactory.parseString("""pusher: {appId: "app0", key: "key0", secret: "secret0"}"""))
      pusher.appId === "app0"
      pusher.key === "key0"
      pusher.secret === "secret0"
    }
  }
  "#trigger" should {
    "make a request to pusher" in {
      val pusher = new PusherClient() {
        override def request(req: HttpRequest) = Future("")
      }

      val res = pusher.trigger("channel", "event", "message", Some("123.234"))
      awaitResult(res) === Result("")
    }
    "without socket" in {
      "make a request to pusher" in {
        val pusher = new PusherClient() {
          override def request(req: HttpRequest) = Future("")
        }

        val res = pusher.trigger("channel", "event", "message")
        awaitResult(res) === Result("")
      }
    }
  }
  "#channel" should {
    "make a request to pusher" in {
      val pusher = new PusherClient() {
        override def request(req: HttpRequest) = Future("{}")
      }

      val res = pusher.channel("channel", Some(Seq("attr1", "attr2")))
      awaitResult(res) === Channel()
    }
    "without attributes" in {
      "make a request to pusher" in {
        val pusher = new PusherClient() {
          override def request(req: HttpRequest) = Future("{}")
        }

        val res = pusher.channel("channel")
        awaitResult(res) === Channel()
      }
    }
  }
  "#channels" should {
    "make a request to pusher" in {
      val pusher = new PusherClient() {
        override def request(req: HttpRequest) = Future("{}")
      }

      val res = pusher.channels("prefix", Some(Seq("attr1", "attr2")))
      awaitResult(res) === Map[String, Channel]()
    }
    "without attributes" in {
      "make a request to pusher" in {
        val pusher = new PusherClient() {
          override def request(req: HttpRequest) = Future("{}")
        }

        val res = pusher.channels("prefix")
        awaitResult(res) === Map[String, Channel]()
      }
    }
  }
  "#users" should {
    "make a request to pusher" in {
      val pusher = new PusherClient() {
        override def request(req: HttpRequest) = Future("""{"users" : []}""")
      }

      val res = pusher.users("channel")
      awaitResult(res) === List[User]()
    }
  }
  "#authenticate" should {
    "return an authenticatedParams" in {
      val pusher = spy(new PusherClient())
      val channelData = ChannelData(
        userId = "test user",
        userInfo = Some(Map("foo" -> "bar"))
      )
      val res = pusher.authenticate("channel", "123.234", Some(channelData))
      res === AuthenticatedParams("key:bd773eb7c2796dcfc240a894f0f4b5a438e901d97d2d474ea9fa34310d3e8357", Some("""{"user_id":"test user","user_info":{"foo":"bar"}}"""))
    }
    "without data" in {
      "return an authenticatedParams" in {
        val pusher = spy(new PusherClient())

        val res = pusher.authenticate("channel", "123.234")
        res === AuthenticatedParams("key:2e3527935cd952830573d54a9199cfac42d5aace747bf301c5517d2da8ef7c38", None)
      }
    }
  }
  "#validateSignature" should {
    "with valid arguments" in {
      "returns true" in {
        val pusher = spy(new PusherClient())

        val res = pusher.validateSignature("key", "773ba44693c7553d6ee20f61ea5d2757a9a4f4a44d2841ae4e95b52e4cd62db4", "foo")
        res === true
      }
    }
    "with invalid key" in {
      "returns false" in {
        val pusher = spy(new PusherClient())

        val res = pusher.validateSignature("invalid", "773ba44693c7553d6ee20f61ea5d2757a9a4f4a44d2841ae4e95b52e4cd62db4", "foo")
        res === false
      }
    }
    "with invalid signature" in {
      "returns false" in {
        val pusher = spy(new PusherClient())

        val res = pusher.validateSignature("key", "invalid", "foo")
        res === false
      }
    }
  }
}
