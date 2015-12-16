package com.github.dtaniwaki.akka_pusher

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.HttpMethods._
import akka.stream.ActorMaterializer
import akka.http.scaladsl.model.MediaTypes._
import com.github.dtaniwaki.akka_pusher.PusherModels._
import com.github.dtaniwaki.akka_pusher.attributes.{PusherChannelAttributes, PusherChannelsAttributes}
import com.typesafe.config.ConfigFactory
import org.mockito.{ArgumentCaptor}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.process.RandomSequentialExecution
import org.specs2.matcher.{Expectable, Matcher}
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Try, Success}

class PusherClientSpec extends Specification
  with RandomSequentialExecution
  with SpecHelper
  with Mockito
{
  implicit val system: ActorSystem = ActorSystem("pusher-client")
  implicit val materializer = ActorMaterializer()

  private def awaitResult[A](future: Future[A]) = Await.result(future, Duration.Inf)

  private def createJsonResponse(responseString: String): Future[(Try[HttpResponse], Int)] =
    Future((Try(new HttpResponse(status = StatusCodes.OK, entity = HttpEntity(ContentType(`application/json`), responseString))), 0))

  private def pusherStub(mockedSource: MockedSource) = new PusherClient() {
    override def source(req: HttpRequest): Future[(Try[HttpResponse], Int)] = mockedSource.hit(req)
  }

  class MockedSource {
    def hit(req: HttpRequest) = createJsonResponse("")
  }

  class HttpGetRequestMatcher(uri: String) extends Matcher[HttpRequest] {
    def apply[S <: HttpRequest](e: Expectable[S]) = {
      val actualReq = e.value
      val actualMethod = e.value.method
      val actualUri = actualReq.uri.toString
      result(
        actualMethod == GET && actualUri.matches(uri),
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
        actualMethod == POST && actualUri.matches(uri) && actualBodyJson == bodyJson,
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
      val mockedSource = mock[MockedSource]
      val argument: ArgumentCaptor[HttpRequest] = ArgumentCaptor.forClass(classOf[HttpRequest])
      mockedSource.hit(argument.capture()) returns createJsonResponse("")

      val pusher = pusherStub(mockedSource)
      val res = pusher.trigger(Seq("channel1", "channel2"), "event", "message", Some("123.234"))
      awaitResult(res) === Success(Result(""))

      argument.getValue() must equalToHttpPostRequest(
        """http://api.pusherapp.com/apps/app/events\?auth_key=key&auth_timestamp=[\d]+&auth_version=1\.0&body_md5=[0-9a-f]+&auth_signature=[0-9a-f]+""",
        """{"data":"\"message\"","name":"event","channels":["channel1","channel2"],"socket_id":"123.234"}""".parseJson
      )
    }
    "without socket" in {
      "make a request to the channels" in {
        val mockedSource = mock[MockedSource]
        val argument: ArgumentCaptor[HttpRequest] = ArgumentCaptor.forClass(classOf[HttpRequest])
        mockedSource.hit(argument.capture()) returns createJsonResponse("")

        val pusher = pusherStub(mockedSource)
        val res = pusher.trigger(Seq("channel1", "channel2"), "event", "message")
        awaitResult(res) === Success(Result(""))

        argument.getValue() must equalToHttpPostRequest(
          """http://api.pusherapp.com/apps/app/events\?auth_key=key&auth_timestamp=[\d]+&auth_version=1\.0&body_md5=[0-9a-f]+&auth_signature=[0-9a-f]+""",
          """{"data":"\"message\"","name":"event","channels":["channel1","channel2"]}""".parseJson
        )
      }
    }
  }
  "#trigger(channel: String, event: String, data: T)" should {
    "make a request to the channel" in {
      val mockedSource = mock[MockedSource]
      val argument: ArgumentCaptor[HttpRequest] = ArgumentCaptor.forClass(classOf[HttpRequest])
      mockedSource.hit(argument.capture()) returns createJsonResponse("")

      val pusher = pusherStub(mockedSource)
      val res = pusher.trigger("channel", "event", "message")
      awaitResult(res) === Success(Result(""))

      argument.getValue() must equalToHttpPostRequest(
        """http://api.pusherapp.com/apps/app/events\?auth_key=key&auth_timestamp=[\d]+&auth_version=1\.0&body_md5=[0-9a-f]+&auth_signature=[0-9a-f]+""",
        """{"data":"\"message\"","name":"event","channels":["channel"]}""".parseJson
      )
    }
  }
  "#trigger(channel: String, event: String, data: T, socketId: Option[String])" should {
    "make a request to the channel" in {
      val mockedSource = mock[MockedSource]
      val argument: ArgumentCaptor[HttpRequest] = ArgumentCaptor.forClass(classOf[HttpRequest])
      mockedSource.hit(argument.capture()) returns createJsonResponse("")

      val pusher = pusherStub(mockedSource)
      val res = pusher.trigger("channel", "event", "message", Some("123.234"))
      awaitResult(res) === Success(Result(""))

      argument.getValue() must equalToHttpPostRequest(
        """(http://api.pusherapp.com/apps/app/events\?auth_key=key&auth_timestamp=[\d]+&auth_version=1\.0&body_md5=[0-9a-f]+&auth_signature=[0-9a-f]+)""",
        """{"data":"\"message\"","name":"event","channels":["channel"],"socket_id":"123.234"}""".parseJson
      )
    }
  }
  "#trigger(Seq((channel: String, event: String, data: T, socketId: Option[String])))" should {
    "make a request to the channels" in {
      val mockedSource = mock[MockedSource]
      val argument: ArgumentCaptor[HttpRequest] = ArgumentCaptor.forClass(classOf[HttpRequest])
      mockedSource.hit(argument.capture()) returns createJsonResponse("")

      val pusher = pusherStub(mockedSource)
      val res = pusher.trigger(Seq(
        ("channel1", "event1", "message1", Some("123.234")),
        ("channel2", "event2", "message2", Some("234.345"))
      ))
      awaitResult(res) === Success(Result(""))

      argument.getValue() must equalToHttpPostRequest(
        """(http://api.pusherapp.com/apps/app/batch_events\?auth_key=key&auth_timestamp=[\d]+&auth_version=1\.0&body_md5=[0-9a-f]+&auth_signature=[0-9a-f]+)""",
        """
          {"batch":[
            {"data":"\"message1\"","name":"event1","channel":"channel1","socket_id":"123.234"},
            {"data":"\"message2\"","name":"event2","channel":"channel2","socket_id":"234.345"}
          ]}
        """.parseJson
      )
    }
  }
  "#channel" should {
    "make a request to pusher" in {
      val mockedSource = mock[MockedSource]
      val argument: ArgumentCaptor[HttpRequest] = ArgumentCaptor.forClass(classOf[HttpRequest])
      mockedSource.hit(argument.capture()) returns createJsonResponse("{}")

      val pusher = pusherStub(mockedSource)
      val res = pusher.channel("channel", Some(Seq(PusherChannelAttributes.subscriptionCount, PusherChannelAttributes.userCount)))
      awaitResult(res) === Success(Channel())

      argument.getValue() must equalToHttpGetRequest(
        """http://api.pusherapp.com/apps/app/channels/channel\?auth_key=key&auth_timestamp=[\d]+&auth_version=1\.0&info=subscription_count,user_count&auth_signature=[0-9a-f]+"""
      )
    }
    "without attributes" in {
      "make a request to pusher" in {
        val mockedSource = mock[MockedSource]
        val argument: ArgumentCaptor[HttpRequest] = ArgumentCaptor.forClass(classOf[HttpRequest])
        mockedSource.hit(argument.capture()) returns createJsonResponse("{}")

        val pusher = pusherStub(mockedSource)
        val res = pusher.channel("channel")
        awaitResult(res) === Success(Channel())

        argument.getValue() must equalToHttpGetRequest(
          """http://api.pusherapp.com/apps/app/channels/channel\?auth_key=key&auth_timestamp=[\d]+&auth_version=1\.0&auth_signature=[0-9a-f]+"""
        )
      }
    }
  }
  "#channels" should {
    "make a request to pusher" in {
      val mockedSource = mock[MockedSource]
      val argument: ArgumentCaptor[HttpRequest] = ArgumentCaptor.forClass(classOf[HttpRequest])
      mockedSource.hit(argument.capture()) returns createJsonResponse("{}")

      val pusher = pusherStub(mockedSource)
      val res = pusher.channels("prefix", Some(Seq(PusherChannelsAttributes.userCount)))
      awaitResult(res) === Success(Map[String, Channel]())

      argument.getValue() must equalToHttpGetRequest(
        """http://api.pusherapp.com/apps/app/channels\?auth_key=key&auth_timestamp=[\d]+&auth_version=1\.0&filter_by_prefix=prefix&info=user_count&auth_signature=[0-9a-f]+"""
      )
    }
    "without attributes" in {
      "make a request to pusher" in {
        val mockedSource = mock[MockedSource]
        val argument: ArgumentCaptor[HttpRequest] = ArgumentCaptor.forClass(classOf[HttpRequest])
        mockedSource.hit(argument.capture()) returns createJsonResponse("{}")

        val pusher = pusherStub(mockedSource)
        val res = pusher.channels("prefix")
        awaitResult(res) === Success(Map[String, Channel]())

        argument.getValue() must equalToHttpGetRequest(
          """http://api.pusherapp.com/apps/app/channels\?auth_key=key&auth_timestamp=[\d]+&auth_version=1\.0&filter_by_prefix=prefix&auth_signature=[0-9a-f]+"""
        )
      }
    }
  }
  "#users" should {
    "make a request to pusher" in {
      val mockedSource = mock[MockedSource]
      val argument: ArgumentCaptor[HttpRequest] = ArgumentCaptor.forClass(classOf[HttpRequest])
      mockedSource.hit(argument.capture()) returns createJsonResponse("""{"users" : []}""")

      val pusher = pusherStub(mockedSource)
      val res = pusher.users("channel")
      awaitResult(res) === Success(List[User]())

      argument.getValue() must equalToHttpGetRequest(
        """http://api.pusherapp.com/apps/app/channels/channel/users\?auth_key=key&auth_timestamp=[\d]+&auth_version=1\.0&auth_signature=[0-9a-f]+"""
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
