package com.github.dtaniwaki.akka_pusher

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.HttpMethods._
import akka.stream.ActorMaterializer
import akka.http.scaladsl.model.MediaTypes._
import akka.stream.scaladsl.Flow
import org.joda.time.DateTimeUtils
import org.specs2.mock.Mockito
import com.github.dtaniwaki.akka_pusher.PusherModels._
import com.github.dtaniwaki.akka_pusher.attributes.{PusherChannelsAttributes, PusherChannelAttributes}
import com.typesafe.config.ConfigFactory
import org.specs2.mutable.Specification
import org.specs2.specification.process.RandomSequentialExecution
import org.specs2.matcher.{Expectable, Matcher}
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Try, Success}
import scala.language.reflectiveCalls

class PusherClientSpec extends Specification
  with RandomSequentialExecution
  with SpecHelper
  with Mockito
  with PusherJsonSupport
{
  implicit val system: ActorSystem = ActorSystem("pusher-client")
  implicit val materializer = ActorMaterializer()
  DateTimeUtils.setCurrentMillisFixed(1452184169130L)

  private def awaitResult[A](future: Future[A]) = Await.result(future, Duration.Inf)

  private def pusherStub(responseBody: String) = new PusherClient() {
    var consumedRequest: HttpRequest = null
    override val pool: Flow[(HttpRequest, Int), (Try[HttpResponse], Int), Any] = Flow[(HttpRequest, Int)].map {
      case (request, n) =>
        consumedRequest = request
        (Success(HttpResponse(status = StatusCodes.OK, entity = HttpEntity(ContentType(`application/json`), responseBody))), n)
    }
  }

  class HttpGetRequestMatcher(uri: String) extends Matcher[HttpRequest] {
    def apply[S <: HttpRequest](e: Expectable[S]) = {
      val actualReq = e.value
      val actualMethod = e.value.method
      val actualUri = actualReq.uri.toString
      result(
        actualMethod == GET && actualUri == uri,
        s"HttpRequest(${actualMethod}, ${actualUri})\n is (GET, ${uri})",
        s"HttpRequest(${actualMethod}, ${actualUri})\n is not (GET, ${uri})",
        e
      )
    }
  }

  class HttpPostRequestMatcher(uri: String, bodyJson: JsValue) extends Matcher[HttpRequest] {
    def apply[S <: HttpRequest](e: Expectable[S]) = {
      val actualReq = e.value
      val actualMethod = e.value.method
      val actualUri = actualReq.uri.toString
      val actualBodyJson = awaitResult(actualReq.entity.toStrict(5 seconds).map(_.data.decodeString("UTF-8"))).parseJson
      result(
        actualMethod == POST && actualUri == uri && actualBodyJson == bodyJson,
        s"HttpRequest(${actualMethod}, ${actualUri}, ${actualBodyJson})\n is (POST, ${uri}, ${bodyJson})",
        s"HttpRequest(${actualMethod}, ${actualUri}, ${actualBodyJson})\n is not (POST, ${uri}, ${bodyJson})",
        e
      )
    }
  }

  private def equalToHttpGetRequest(uri: String) = new HttpGetRequestMatcher(uri)
  private def equalToHttpPostRequest(uri: String, jsonBody: JsValue) = new HttpPostRequestMatcher(uri, jsonBody)

  "#constructor" should {
    "accept the config by argument" in {
      val pusher = new PusherClient(ConfigFactory.parseString("""pusher: {appId: "app0", key: "key0", secret: "secret0"}"""))
      pusher.appId === "app0"
      pusher.key === "key0"
      pusher.secret === "secret0"
    }
  }

  "#trigger(channels: Seq[String], event: String, data: T, socketId: Option[String] = None)" should {
    "make a request to the the channels" in {
      val pusher = pusherStub("")
      val res = pusher.trigger(Seq("channel1", "channel2"), "event", "message", Some("123.234"))
      awaitResult(res) === Success(Result(""))

      pusher.consumedRequest must equalToHttpPostRequest(
        """http://api.pusherapp.com/apps/app/events?auth_key=key&auth_timestamp=1452184169&auth_version=1.0&body_md5=567be22be06070b3cf618f8bc59efa74&auth_signature=59a8f724d3487bde7dcc51a13d9b6d2747fb80d22ed824ada51d5b0e60f42c1e""",
        """{"data":"\"message\"","name":"event","channels":["channel1","channel2"],"socket_id":"123.234"}""".parseJson
      )
    }
    "without socket" in {
      "make a request to the channels" in {
        val pusher = pusherStub("")
        val res = pusher.trigger(Seq("channel1", "channel2"), "event", "message")
        awaitResult(res) === Success(Result(""))

        pusher.consumedRequest must equalToHttpPostRequest(
          """http://api.pusherapp.com/apps/app/events?auth_key=key&auth_timestamp=1452184169&auth_version=1.0&body_md5=a263789fb4fa6eea04c6f0f1ed57c827&auth_signature=5092137acbdb89a5c5f3d3b69b359c19c87b11a7e3e5492559d366ce011e259b""",
          """{"data":"\"message\"","name":"event","channels":["channel1","channel2"]}""".parseJson
        )
      }
    }
  }
  "#trigger(channel: String, event: String, data: T)" should {
    "make a request to the channel" in {
      val pusher = pusherStub("")
      val res = pusher.trigger("channel", "event", "message")
      awaitResult(res) === Success(Result(""))

      pusher.consumedRequest must equalToHttpPostRequest(
        """http://api.pusherapp.com/apps/app/events?auth_key=key&auth_timestamp=1452184169&auth_version=1.0&body_md5=4caaeb8a1a2a881977ab8dbbedb44165&auth_signature=edf2aa45294d7128b4d4669d9583eb27f5fb481e064668efea4eca955588cbf6""",
        """{"data":"\"message\"","name":"event","channels":["channel"]}""".parseJson
      )
    }
  }
  "#trigger(channel: String, event: String, data: T, socketId: Option[String])" should {
    "make a request to the channel" in {
      val pusher = pusherStub("")
      val res = pusher.trigger("channel", "event", "message", Some("123.234"))
      awaitResult(res) === Success(Result(""))

      pusher.consumedRequest must equalToHttpPostRequest(
        """http://api.pusherapp.com/apps/app/events?auth_key=key&auth_timestamp=1452184169&auth_version=1.0&body_md5=9f368a70902d7977ad32b4fdd01e8929&auth_signature=cfdafb4b8f6cfd6ffe69f8531f7fa9411c01a49617ccab74ead60715f3453fdf""",
        """{"data":"\"message\"","name":"event","channels":["channel"],"socket_id":"123.234"}""".parseJson
      )
    }
  }
  "#trigger(Seq((channel: String, event: String, data: T, socketId: Option[String])))" should {
    "make a request to the channels" in {
      val pusher = pusherStub("")
      val res = pusher.trigger(Seq(
        ("channel1", "event1", "message1", Some("123.234")),
        ("channel2", "event2", "message2", Some("234.345"))
      ))
      awaitResult(res) === Success(Result(""))

      pusher.consumedRequest must equalToHttpPostRequest(
        """http://api.pusherapp.com/apps/app/batch_events?auth_key=key&auth_timestamp=1452184169&auth_version=1.0&body_md5=27ca0add3a65dd8d8300d03f11c2cfdb&auth_signature=e5db0134b6da74adff472638f77d2f2f73d0f259584701995afc49ae33d66d33""",
        """
          {"batch":[
            {"data":"\"message1\"","name":"event1","channel":"channel1","socket_id":"123.234"},
            {"data":"\"message2\"","name":"event2","channel":"channel2","socket_id":"234.345"}
          ]}
        """.parseJson
      )
    }
  }
  "#channel(channelName: String, attributes: Seq[PusherChannelAttributes.Value] = Seq())" should {
    "make a request to pusher" in {
      val pusher = pusherStub("{}")
      val res = pusher.channel("channel", Seq(PusherChannelAttributes.subscriptionCount, PusherChannelAttributes.userCount))
      awaitResult(res) === Success(Channel())

      pusher.consumedRequest must equalToHttpGetRequest(
        """http://api.pusherapp.com/apps/app/channels/channel?auth_key=key&auth_timestamp=1452184169&auth_version=1.0&info=subscription_count,user_count&auth_signature=135e1dada101b10e127f5ba7bfbbf810d24463fb0820922ba781a5cc47bf633e"""
      )
    }
    "without attributes" in {
      "make a request to pusher" in {
        val pusher = pusherStub("{}")
        val res = pusher.channel("channel")
        awaitResult(res) === Success(Channel())

        pusher.consumedRequest must equalToHttpGetRequest(
          """http://api.pusherapp.com/apps/app/channels/channel?auth_key=key&auth_timestamp=1452184169&auth_version=1.0&auth_signature=4898a6b178f2a53a88285f4f937bdce663dc43aa942dcb84fb84d86ec904c6aa"""
        )
      }
    }
  }
  "(deprecated) #channel(channelName: String, attributes: Option[Seq[String]])" should {
    "call the new channel function" in {
      val clientMock = mock[PusherClient]
      clientMock.channel(anyString, anyListOf[PusherChannelAttributes.Value]) returns Future(Success(Channel()))
      clientMock.channel("channel", Seq(PusherChannelAttributes.userCount))
      there was one(clientMock).channel("channel", Seq(PusherChannelAttributes.userCount))
    }
  }
  "#channels(prefixFilter: String, attributes: Seq[PusherChannelsAttributes.Value] = Seq())" should {
    "make a request to pusher" in {
      val pusher = pusherStub("{}")
      val res = pusher.channels("prefix", Seq(PusherChannelsAttributes.userCount))
      awaitResult(res) === Success(ChannelMap())

      pusher.consumedRequest must equalToHttpGetRequest(
        """http://api.pusherapp.com/apps/app/channels?auth_key=key&auth_timestamp=1452184169&auth_version=1.0&filter_by_prefix=prefix&info=user_count&auth_signature=5fbace1f69182f30f977fb6f7004fcab587fd2339fe3f5de4288ff734a86cf6e"""
      )
    }
    "without attributes" in {
      "make a request to pusher" in {
        val pusher = pusherStub("{}")
        val res = pusher.channels("prefix")
        awaitResult(res) === Success(ChannelMap())

        pusher.consumedRequest must equalToHttpGetRequest(
          """http://api.pusherapp.com/apps/app/channels?auth_key=key&auth_timestamp=1452184169&auth_version=1.0&filter_by_prefix=prefix&auth_signature=748998ffe9e177acfea1b7cb8213f5c969b79b9aa0d54e477538e26c5b998bf9"""
        )
      }
    }
  }
  "(deprecated) #channels(prefixFilter: String, attributes: Option[Seq[String]])" should {
    "call the new channel function" in {
      val clientMock = mock[PusherClient]
      clientMock.channels(anyString, Seq(any)) returns Future(Success(ChannelMap()))
      clientMock.channels("channel", Seq(PusherChannelsAttributes.userCount))
      there was one(clientMock).channels("channel", Seq(PusherChannelsAttributes.userCount))
    }
  }
  "#users" should {
    "make a request to pusher" in {
      val pusher = pusherStub("""{"users" : []}""")
      val res = pusher.users("channel")
      awaitResult(res) === Success(UserList())

      pusher.consumedRequest must equalToHttpGetRequest(
        """http://api.pusherapp.com/apps/app/channels/channel/users?auth_key=key&auth_timestamp=1452184169&auth_version=1.0&auth_signature=04baeea473d69c1c104b4b306c1fde000f75b2baf9a39a50d01d7fc5d9c80268"""
      )
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
  "#shutdown" should {
    "shutdown" in {
      val pusher = new PusherClient()

      {
        pusher.shutdown()
      } must not(throwA[Exception])
    }
  }
}
