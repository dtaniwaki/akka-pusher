package com.github.dtaniwaki.akka_pusher

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{ Sink, Source }
import com.github.dtaniwaki.akka_pusher.PusherExceptions._
import com.github.dtaniwaki.akka_pusher.PusherModels._
import com.github.dtaniwaki.akka_pusher.Utils._
import com.typesafe.config.{ Config, ConfigFactory }
import net.ceedubs.ficus.Ficus._
import spray.http.Uri
import spray.json._
import org.slf4j.LoggerFactory
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{ Try, Success }

class PusherClient(config: Config = ConfigFactory.load())(implicit val system: ActorSystem = ActorSystem("pusher-client"))
    extends PusherJsonSupport
    with PusherValidator {
  private lazy val logger = LoggerFactory.getLogger(getClass)

  val host = config.as[Option[String]]("pusher.host").getOrElse("api.pusherapp.com").trim()
  val appId = config.getString("pusher.appId").trim()
  val key = config.getString("pusher.key").trim()
  val secret = config.getString("pusher.secret").trim()
  val ssl = config.as[Option[Boolean]]("pusher.ssl").getOrElse(false)
  logger.debug("PusherClient configuration:")
  logger.debug(s"appId........ ${appId}")
  logger.debug(s"key.......... ${key}")
  logger.debug("secret....... <masked>")
  logger.debug(s"ssl.......... ${ssl}")

  implicit val materializer = ActorMaterializer()(system)
  private val (pool, scheme) = if (ssl) {
    (Http(system).cachedHostConnectionPoolTls[Int](host), "https")
  } else {
    (Http(system).cachedHostConnectionPool[Int](host), "http")
  }
  implicit val ec: ExecutionContext = system.dispatcher

  def trigger[T: JsonWriter](channels: Seq[String], event: String, data: T, socketId: Option[String] = None): Future[Result] = {
    channels.foreach(validateChannel)
    socketId.map(validateSocketId(_))
    var uri = generateUri(path = Uri.Path(s"/apps/$appId/events"))

    val body = Map[String, JsValue](
      "data" -> JsString(data.toJson.compactPrint),
      "name" -> JsString(event),
      "channels" -> JsArray(channels.map(JsString.apply).toVector),
      "socket_id" -> socketId.map(JsString(_)).getOrElse(JsNull)
    ).filter(_._2 != JsNull).toJson.compactPrint

    uri = signUri("POST", uri, Some(body))

    request(HttpRequest(method = POST, uri = uri.toString, entity = HttpEntity(ContentType(`application/json`), body))).map { new Result(_) }
  }
  def trigger[T: JsonWriter](channel: String, event: String, data: T): Future[Result] = trigger(channel, event, data, None)
  def trigger[T: JsonWriter](channel: String, event: String, data: T, socketId: Option[String]): Future[Result] = trigger(Seq(channel), event, data, socketId)

  def channel(channel: String, attributes: Option[Seq[String]] = None): Future[Channel] = {
    validateChannel(channel)
    var uri = generateUri(path = Uri.Path(s"/apps/$appId/channels/$channel"))

    val params = Map(
      "info" -> attributes.map(_.mkString(","))
    ).filter(_._2.isDefined).mapValues(_.get)

    uri = signUri("GET", uri.withQuery(params))

    request(HttpRequest(method = GET, uri = uri.toString)).map(_.parseJson.convertTo[Channel])
  }

  def channels(prefixFilter: String, attributes: Option[Seq[String]] = None): Future[Map[String, Channel]] = {
    var uri = generateUri(path = Uri.Path(s"/apps/$appId/channels"))

    val params = Map(
      "filter_by_prefix" -> Some(prefixFilter),
      "info" -> attributes.map(_.mkString(","))
    ).filter(_._2.isDefined).mapValues(_.get)

    uri = signUri("GET", uri.withQuery(params))

    request(HttpRequest(method = GET, uri = uri.toString)).map(_.parseJson.convertTo[Map[String, Channel]])
  }

  def users(channel: String): Future[List[User]] = {
    validateChannel(channel)
    var uri = generateUri(path = Uri.Path(s"/apps/$appId/channels/$channel/users"))
    uri = signUri("GET", uri)

    request(HttpRequest(method = GET, uri = uri.toString)).map(_.parseJson.convertTo[List[User]])
  }

  def authenticate[T: JsonWriter](channel: String, socketId: String, data: Option[ChannelData[T]] = Option.empty[ChannelData[String]]): AuthenticatedParams = {
    val serializedData = data.map(_.toJson.compactPrint)
    val signingStrings = serializedData.foldLeft(List(socketId, channel))(_ :+ _)
    AuthenticatedParams(s"$key:${signature(signingStrings.mkString(":"))}", serializedData)
  }

  def validateSignature(_key: String, _signature: String, body: String): Boolean = {
    key == _key && signature(body) == _signature
  }

  protected def source(req: HttpRequest): Future[(Try[HttpResponse], Int)] = {
    Source.single(req, 0)
      .via(pool)
      .runWith(Sink.head)
  }

  protected def request(req: HttpRequest): Future[String] = {
    source(req)
      .flatMap {
        case (Success(response), _) =>
          response.entity.withContentType(ContentTypes.`application/json`)
            .toStrict(5 seconds)
            .map(_.data.decodeString(response.entity.contentType.charset.value))
            .map { body =>
              response.status match {
                case StatusCodes.OK           => body
                case StatusCodes.BadRequest   => throw new BadRequest(body)
                case StatusCodes.Unauthorized => throw new Unauthorized(body)
                case StatusCodes.Forbidden    => throw new Forbidden(body)
                case _                        => throw new PusherException(body)
              }
            }
        case _ =>
          throw new PusherException("Pusher request failed")
      }
  }

  private def generateUri(path: Uri.Path): Uri = {
    Uri(scheme = scheme, authority = Uri.Authority(Uri.Host(host)), path = path)
  }

  private def signUri(method: String, uri: Uri, data: Option[String] = None): Uri = {
    var signedUri = uri
    var params = List(
      ("auth_key", key),
      ("auth_timestamp", (System.currentTimeMillis / 1000).toString),
      ("auth_version", "1.0")
    )
    if (data.isDefined) {
      val serializedData = data.get
      params = params :+ ("body_md5", md5(serializedData))
    }
    signedUri = signedUri.withQuery(params ++ uri.query.toList: _*)

    val signingString = s"$method\n${uri.path}\n${signedUri.query.toString}"
    signedUri.withQuery(signedUri.query.toList :+ ("auth_signature", signature(signingString)): _*)
  }

  private def signature(value: String): String = {
    sha256(secret, value)
  }

  def shutdown(): Unit = {
    Http(system).shutdownAllConnectionPools()
  }
}
