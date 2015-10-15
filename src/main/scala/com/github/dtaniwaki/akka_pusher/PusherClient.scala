package com.github.dtaniwaki.akka_pusher

import spray.json._
import spray.http.Uri
import scala.concurrent.{ ExecutionContext, Future, Promise, Await }
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{ Failure, Success, Try }
import akka.actor.ActorSystem
import akka.stream.{Materializer, ActorMaterializer}
import akka.stream.scaladsl.{ Source, Flow, Sink }
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.Http
import akka.http.scaladsl.HttpsContext
import HttpMethods._
import HttpProtocols._
import MediaTypes._
import com.typesafe.config.{ConfigFactory, Config}
import com.typesafe.scalalogging.StrictLogging

import Utils._
import PusherModels._
import PusherExceptions._

class PusherClient(config: Config = ConfigFactory.load())(implicit val system: ActorSystem = ActorSystem("pusher")) extends PusherJsonSupport
  with StrictLogging
  with PusherValidator
{
  private val host = "api.pusherapp.com"
  val appId = config.getString("pusher.appId")
  val key = config.getString("pusher.key")
  val secret = config.getString("pusher.secret")
  private val ssl = if (config.hasPath("pusher.ssl"))
    config.getBoolean("pusher.ssl")
  else
    false

  implicit val materializer = ActorMaterializer()
  lazy val pool = if (ssl)
    Http(system).cachedHostConnectionPoolTls[Int](host)
  else
    Http(system).newHostConnectionPool[Int](host)

  def trigger(event: String, channel: String, message: String, socketId: Option[String] = None): Future[Result] = {
    validateChannel(channel)
    socketId.map(validateSocketId(_))
    var uri = Uri(authority = Uri.Authority(Uri.Host(host)), path = Uri.Path(s"/apps/$appId/events"))

    val data = Map(
      "data" -> Some(Map("message" -> message).toJson.toString),
      "name" -> Some(event),
      "channel" -> Some(channel),
      "socket_id" -> socketId
    ).filter(_._2.isDefined).mapValues(_.get)

    uri = signUri("POST", uri, Some(data))

    Source.single(HttpRequest(method = POST, uri = uri.toString, entity = HttpEntity(ContentType(`application/json`), data.toJson.toString)), 0)
    .via(pool).runWith(Sink.head).flatMap {
      case (Success(resp), _) =>
        resp.entity.toStrict(5 seconds).map(_.data.decodeString("UTF-8")).map{ new Result(_) }
      case _ => throw new PusherException("Pusher request failed")
    }
  }

  def channel(channel: String, attributes: Option[Seq[String]] = None): Future[Channel] = {
    validateChannel(channel)
    var uri = Uri(authority = Uri.Authority(Uri.Host(host)), path = Uri.Path(s"/apps/$appId/channels/${channel}"))

    val params = Map(
      "info" -> attributes.map(_.mkString(","))
    ).filter(_._2.isDefined).mapValues(_.get)

    uri = signUri("GET", uri.withQuery(params))

    Source.single(HttpRequest(method = GET, uri = uri.toString), 0)
    .via(pool).runWith(Sink.head).flatMap {
      case (Success(resp), _) =>
        resp.entity.toStrict(5 seconds).map(_.data.decodeString("UTF-8")).map{ new Channel(_) }
      case _ => throw new PusherException("Pusher request failed")
    }
  }

  def channels(prefixFilter: String, attributes: Option[Seq[String]] = None): Future[Channels] = {
    var uri = Uri(authority = Uri.Authority(Uri.Host(host)), path = Uri.Path(s"/apps/$appId/channels"))

    val params = Map(
      "filter_by_prefix" -> Some(prefixFilter),
      "info" -> attributes.map(_.mkString(","))
    ).filter(_._2.isDefined).mapValues(_.get)

    uri = signUri("GET", uri.withQuery(params))

    Source.single(HttpRequest(method = GET, uri = uri.toString), 0)
    .via(pool).runWith(Sink.head).flatMap {
      case (Success(resp), _) =>
        resp.entity.toStrict(5 seconds).map(_.data.decodeString("UTF-8")).map{ new Channels(_) }
      case _ => throw new PusherException("Pusher request failed")
    }
  }

  def users(channel: String): Future[Users] = {
    validateChannel(channel)
    var uri = Uri(authority = Uri.Authority(Uri.Host(host)), path = Uri.Path(s"/apps/$appId/channels/${channel}/users"))
    uri = signUri("GET", uri)

    Source.single(HttpRequest(method = GET, uri = uri.toString), 0)
    .via(pool).runWith(Sink.head).flatMap {
      case (Success(resp), _) =>
        resp.entity.toStrict(5 seconds).map(_.data.decodeString("UTF-8")).map{ new Users(_) }
      case _ => throw new PusherException("Pusher request failed")
    }
  }

  def authenticate(channel: String, socketId: String, data: Option[Map[String, String]] = None): AuthenticatedParams = {
    var list = List(socketId, channel)
    var serializedData: Option[String] = None
    var signingStrings = List(socketId, channel)
    data.map { data =>
      val _serializedData = data.toJson.toString
      signingStrings = signingStrings :+ _serializedData
      serializedData = Some(_serializedData)
    }
    AuthenticatedParams(s"${key}:${signature(signingStrings.mkString(":"))}", serializedData)
  }

  def validateSignature(_key: String, _signature: String, body: String): Boolean = {
    key == _key && signature(body) == _signature
  }

  private def signUri(method: String, uri: Uri, data: Option[Map[String, String]] = None): Uri = {
    var signedUri = uri
    var params = List(
      ("auth_key", key),
      ("auth_timestamp", (System.currentTimeMillis() / 1000).toString),
      ("auth_version", "1.0")
    )
    if (data.isDefined) {
      val serializedData = data.get.toJson.toString
      params = params :+ ("body_md5", md5(serializedData))
    }
    signedUri = signedUri.withQuery((params ++ uri.query.toList): _*)

    val signingString = s"$method\n${uri.path}\n${signedUri.query.toString}"
    signedUri.withQuery(signedUri.query.toList :+ ("auth_signature", signature(signingString)): _*)
  }

  private def signature(value: String): String = {
    sha256(secret, value)
  }

  def shutdown() = {
    system.shutdown()
  }
}
