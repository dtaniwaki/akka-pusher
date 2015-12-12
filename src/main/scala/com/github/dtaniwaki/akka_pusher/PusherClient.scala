package com.github.dtaniwaki.akka_pusher

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethod
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
import akka.http.scaladsl.model.Uri
import spray.json._
import org.slf4j.LoggerFactory
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{ Try, Success, Failure }

class PusherClient(config: Config = ConfigFactory.load())(implicit val system: ActorSystem = ActorSystem("pusher-client"))
    extends PusherJsonSupport
    with PusherValidator {
  private lazy val logger = LoggerFactory.getLogger(getClass)
  private val defaultHeaders: List[HttpHeader] = List(headers.`User-Agent`(s"akka-pusher v${getClass.getPackage.getImplementationVersion}"))

  val host: String = config.as[Option[String]]("pusher.host").getOrElse("api.pusherapp.com").trim()
  val appId: String = config.getString("pusher.appId").trim()
  val key: String = config.getString("pusher.key").trim()
  val secret: String = config.getString("pusher.secret").trim()
  val ssl: Boolean = config.as[Option[Boolean]]("pusher.ssl").getOrElse(false)
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

  def trigger[T: JsonWriter](channels: Seq[String], event: String, data: T, socketId: Option[String] = None): Future[Try[Result]] = {
    validateChannel(channels)
    validateSocketId(socketId)
    var uri = generateUri(s"/apps/$appId/events")

    val body = Map[String, JsValue](
      "data" -> JsString(data.toJson.compactPrint),
      "name" -> JsString(event),
      "channels" -> JsArray(channels.map(JsString.apply).toVector),
      "socket_id" -> socketId.map(JsString(_)).getOrElse(JsNull)
    ).filter(_._2 != JsNull).toJson.compactPrint

    uri = signUri("POST", uri, Some(body))

    request(method = POST, uri = uri.toString, entity = HttpEntity(ContentType(`application/json`), body)).map(_.map {
      logger.debug(s"Sent 1 event: ${event}")
      new Result(_)
    })
  }
  def trigger[T: JsonWriter](channel: String, event: String, data: T): Future[Try[Result]] = trigger(channel, event, data, None)
  def trigger[T: JsonWriter](channel: String, event: String, data: T, socketId: Option[String]): Future[Try[Result]] = trigger(Seq(channel), event, data, socketId)
  def trigger[T: JsonWriter](triggers: Seq[(String, String, T, Option[String])]): Future[Try[Result]] = {
    validateTriggers(triggers)
    var uri = generateUri(s"/apps/$appId/batch_events")

    val body = JsObject("batch" -> JsArray(triggers.map {
      case (channel, event, data, socketId) =>
        Map[String, JsValue](
          "data" -> JsString(data.toJson.compactPrint),
          "name" -> JsString(event),
          "channel" -> JsString(channel),
          "socket_id" -> socketId.map(JsString(_)).getOrElse(JsNull)
        ).filter(_._2 != JsNull).toJson
    }.toVector)).compactPrint

    uri = signUri("POST", uri, Some(body))
    request(method = POST, uri = uri.toString, entity = HttpEntity(ContentType(`application/json`), body)).map(_.map { res =>
      logger.debug(s"Sent ${triggers.length} events: ${triggers.slice(0, 3).map(_._2).mkString(", ")} ...")
      new Result(res)
    })
  }

  def channel(channel: String, attributes: Option[Seq[String]] = None): Future[Try[Channel]] = {
    validateChannel(channel)
    var uri = generateUri(s"/apps/$appId/channels/$channel")

    val params = Map(
      "info" -> attributes.map(_.mkString(","))
    ).filter(_._2.isDefined).mapValues(_.get)

    uri = signUri("GET", uri.withQuery(params))

    request(method = GET, uri = uri.toString).map(_.map(_.parseJson.convertTo[Channel]))
  }

  def channels(prefixFilter: String, attributes: Option[Seq[String]] = None): Future[Try[Map[String, Channel]]] = {
    var uri = generateUri(s"/apps/$appId/channels")

    val params = Map(
      "filter_by_prefix" -> Some(prefixFilter),
      "info" -> attributes.map(_.mkString(","))
    ).filter(_._2.isDefined).mapValues(_.get)

    uri = signUri("GET", uri.withQuery(params))

    request(method = GET, uri = uri.toString).map(_.map(_.parseJson.convertTo[Map[String, Channel]]))
  }

  def users(channel: String): Future[Try[List[User]]] = {
    validateChannel(channel)
    var uri = generateUri(s"/apps/$appId/channels/$channel/users")
    uri = signUri("GET", uri)

    request(method = GET, uri = uri.toString).map(_.map(_.parseJson.convertTo[List[User]]))
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

  protected def request(method: HttpMethod, uri: String, entity: RequestEntity = HttpEntity.Empty): Future[Try[String]] = {
    source(HttpRequest(method = method, uri = uri, entity = entity, headers = defaultHeaders))
      .flatMap {
        case (Success(response), _) =>
          response.entity.withContentType(ContentTypes.`application/json`)
            .toStrict(5 seconds)
            .map(_.data.decodeString(response.entity.contentType.charset.value))
            .map { body =>
              response.status match {
                case StatusCodes.OK           => Success(body)
                case StatusCodes.BadRequest   => Failure(new BadRequest(body))
                case StatusCodes.Unauthorized => Failure(new Unauthorized(body))
                case StatusCodes.Forbidden    => Failure(new Forbidden(body))
                case _                        => Failure(new PusherException(body))
              }
            }
        case _ =>
          Future(Failure(new PusherException("Pusher request failed")))
      }
  }

  private def generateUri(path: String): Uri = {
    Uri(scheme = scheme, authority = Uri.Authority(Uri.Host(host)), path = Uri.Path(path))
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
