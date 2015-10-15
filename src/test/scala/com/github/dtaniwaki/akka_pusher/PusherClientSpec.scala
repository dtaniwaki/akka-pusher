package com.github.dtaniwaki.akka_pusher

import org.specs2.mutable.Specification
import org.specs2.specification.process.RandomSequentialExecution
import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory

import PusherModels._
import PusherExceptions._

class PusherClientSpec extends Specification
  with RandomSequentialExecution
{
  private def awaitResult[A](future: Future[A]) = Await.result(future, Duration.Inf)
  val pusher = new PusherClient()

  "#constructor" should {
    "accept the config by argument" in {
      val pusher = new PusherClient(ConfigFactory.parseString("""pusher: {appId: "app0", key: "key0", secret: "secret0"}"""))
      pusher.appId === "app0"
      pusher.key === "key0"
      pusher.secret === "secret0"
    }
  }
  // "#trigger" should {
  //   "make a request to pusher" in {
  //     val res = pusher.trigger("event", "channel", "message", Some("123.234"))
  //     awaitResult(res) === Result("")
  //   }
  //   "without socket" in {
  //     "make a request to pusher" in {
  //       val res = pusher.trigger("event", "channel", "message")
  //       awaitResult(res) === Result("")
  //     }
  //   }
  //   "request failed" in {
  //     "throws an exception" in {
  //       {
  //         pusher.trigger("event", "channel", "message")
  //       } must throwA(new PusherException(s"Pusher request failed"))
  //     }
  //   }
  // }
  // "#channel" should {
  //   "make a request to pusher" in {
  //     val res = pusher.channel("channel", Some(Seq("attr1", "attr2")))
  //     awaitResult(res) === Channel("")
  //   }
  //   "without attributes" in {
  //     "make a request to pusher" in {
  //       val res = pusher.channel("channel")
  //       awaitResult(res) === Channel("")
  //     }
  //   }
  //   "request failed" in {
  //     "returns a pusher error" in {
  //       {
  //         pusher.channel("channel")
  //       } must throwA(new PusherException(s"Pusher request failed"))
  //     }
  //   }
  // }
  // "#channels" should {
  //   "make a request to pusher" in {
  //     val res = pusher.channels("prefix", Some(Seq("attr1", "attr2")))
  //     awaitResult(res) === Channels("")
  //   }
  //   "without attributes" in {
  //     "make a request to pusher" in {
  //       val res = pusher.channels("prefix")
  //       awaitResult(res) === Channels("")
  //     }
  //   }
  //   "request failed" in {
  //     "returns a pusher error" in {
  //       {
  //         pusher.channels("prefix")
  //       } must throwA(new PusherException(s"Pusher request failed"))
  //     }
  //   }
  // }
  // "#users" should {
  //   "make a request to pusher" in {
  //     val res = pusher.users("channel")
  //     awaitResult(res) === Users("")
  //   }
  //   "request failed" in {
  //     "returns a pusher error" in {
  //       {
  //         pusher.users("channel")
  //       } must throwA(new PusherException(s"Pusher request failed"))
  //     }
  //   }
  // }
  "#authenticate" should {
    "return an authenticatedParams" in {
      val res = pusher.authenticate("channel", "123.234", Some(Map("foo" -> "bar")))
      res === AuthenticatedParams("key:89728bf2c4f1c2ba483805632d85848f6cb350777a00632ec45958c616978414", Some("""{"foo":"bar"}"""))
    }
    "without data" in {
      "return an authenticatedParams" in {
        val res = pusher.authenticate("channel", "123.234")
        res === AuthenticatedParams("key:2e3527935cd952830573d54a9199cfac42d5aace747bf301c5517d2da8ef7c38", None)
      }
    }
  }
  "#validateSignature" should {
    "with valid arguments" in {
      "returns true" in {
        val res = pusher.validateSignature("key", "773ba44693c7553d6ee20f61ea5d2757a9a4f4a44d2841ae4e95b52e4cd62db4", "foo")
        res === true
      }
    }
    "with invalid key" in {
      "returns false" in {
        val res = pusher.validateSignature("invalid", "773ba44693c7553d6ee20f61ea5d2757a9a4f4a44d2841ae4e95b52e4cd62db4", "foo")
        res === false
      }
    }
    "with invalid signature" in {
      "returns false" in {
        val res = pusher.validateSignature("key", "invalid", "foo")
        res === false
      }
    }
  }
}
