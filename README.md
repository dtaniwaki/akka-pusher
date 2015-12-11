# akka-pusher

[![Maven Central][maven-image]][maven-link]
[![Coverage][coverage-image]][coverage-link]
[![CI][ci-image]][ci-link]
[![License: MIT](http://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

[Pusher](https://pusher.com/) Client under Akka's actor context.

## Prerequisites

- Java 8 or higher
- Scala 2.10 and Scala 2.11

## Installation

Add the following to your sbt build (Scala 2.11.x):

```scala
libraryDependencies += "com.github.dtaniwaki" %% "akka-pusher" % "x.y.z"
```

Here, `x.y.z` is the akka-pusher package version you want to use.

## Usage

```scala
import com.github.dtaniwaki.akka_pusher.PusherClient

implicit val system = ActorSystem("pusher")
val pusher = new PusherClient()
pusher.trigger("test_channel", "my_event", "hello world")
pusher.shutdown()
```

If you want to run another actor for pusher,

```scala
import com.github.dtaniwaki.akka_pusher.PusherActor
import com.github.dtaniwaki.akka_pusher.PusherMessages._

val system = ActorSystem("pusher")
val pusherActor = system.actorOf(PusherActor.props(), "pusher-actor")
pusherActor ! TriggerMessage("test_channel", "my_event", "hello world")
```

A working sample is available [here](https://github.com/dtaniwaki/akka-pusher-play-app).

### API

#### trigger

```scala
val result: Future[Try[Result]] = pusher.trigger("test_channel", "my_event", Map("foo" -> "bar"))
```

#### batch trigger

```scala
val result: Future[Try[Result]] = pusher.trigger(Seq(("test_channel", "my_event", Map("foo" -> "bar"))))
```

#### channels

```scala
val channels: Future[Try[Map[String, Channel]]] = pusher.channels("my_")
```

#### channel

```scala
val channel: Future[Try[Channel]] = pusher.channel("my_channel")
```

#### users

```scala
val users: Future[Try[List[User]]] = pusher.users("my_channel")
```

#### authenticate

```scala
case class Foo(body: String)
implicit val FooJsonSupport = jsonFormat1(Foo)
val channelData: ChannelData[Foo] = ChannelData("user_id", Foo("body"))
val params: AuthenticatedParams = authenticate("my_channel", "socket_id", Some(channelData))
```

#### validateSignature

```scala
val valid: Signature = validateSignature("pusher_key", "pusher_signature", "body")
```

### Akka Actor Messages

#### TriggerMessage

```scala
(pusherActor ask TriggerMessage("channel-name", "event-name", "JSON OBJECT".toJson, Some("123.345"))).map {
  case Success(res: PusherModels.Result) => println(res)
  case Failure(e) => throw e
}
```

#### BatchTriggerMessage

Only availbale if the `batchTrigger` is `true` in the config file.

```scala
pusherActor ! BatchTriggerMessage(channel, event, body.toJson, socketId)
```

The trigger will be executed in batch in 1000 milliseconds (default).

#### ChannelMessage

```scala
(pusherActor ask ChannelMessage("channel-name", Some(Seq("user_count")))).map {
  case Success(res: PusherModels.Channel) => println(res)
  case Failure(e) => throw e
}
```

#### ChannelsMessage

```scala
(pusherActor ask ChannelsMessage("channel-name-prefix", Some(Seq("user_count")))).map {
  case Success(res: Map[_, _]) if res.forall{ case (k, v) => k.isInstanceOf[String] && v.isInstanceOf[PusherModels.Channel] } =>
    println(res.asInstanceOf[Map[String, PusherModels.Channel]])
  case Failure(e) => throw e
}
```

####

```scala
(pusherActor ask UsersMessage("channel-name")).map {
  case Success(res: List[_]) if res.forall(_.isInstanceOf[PusherModels.User]) =>
    println(res.asInstanceOf[List[PusherModels.User]])
  case Failure(e) => throw e
}
```

#### AuthenticateMessage

```scala
val pusherRequest = AuthRequest()
(pusherActor ask AuthenticateMessage(
  "channel-name",
  Some("123.345"),
  Some(PusherModels.ChannelData(userId = "dtaniwaki", userInfo = Some(Map("user_name" -> "dtaniwaki", "name" -> "Daisuke Taniwaki").toJson)))
)).map {
  case res: PusherModels.AuthenticatedParams =>
    println(res)
}
```

#### ValidateSignatureMessage

```scala
(pusherActor ask ValidateSignatureMessage(key, signature, request.body.toString)).map {
  case Success(res) =>
    println(res)
  case Failure(e) =>
    throw e
}
```

## Configuration

PusherClient use `application.conf` parsed by [typesafe config](https://github.com/typesafehub/config).

```
pusher {
  appId=${?PUSHER_APP_ID}
  key=${?PUHSER_API_KEY}
  secret=${?PUSHER_API_SECRET}
}
```

Here, you can replace the variables or set them as environment variables.

Or, you can directly set the config by the costructor argument.

```scala
val pusher = new PusherClient(ConfigFactory.parseString("""pusher: {appId: "app0", key: "key0", secret: "secret0"}"""))
```

## Test

```bash
sbt test
```

### Coverage

```bash
sbt clean coverage test
```

![codecov.io](https://codecov.io/github/dtaniwaki/akka-pusher/branch.svg?branch=master)

## Contributing

1. Fork it
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Add some feature'`)
4. Push to the branch (`git push origin my-new-feature`)
5. Create new [Pull Request](../../pull/new/master)

## Copyright

Copyright (c) 2015 Daisuke Taniwaki. See [LICENSE](LICENSE) for details.

[ci-image]:  https://travis-ci.org/dtaniwaki/akka-pusher.svg
[ci-link]:   https://travis-ci.org/dtaniwaki/akka-pusher
[maven-image]:  https://maven-badges.herokuapp.com/maven-central/com.github.dtaniwaki/akka-pusher_2.11/badge.svg?style=plastic
[maven-link]:   https://maven-badges.herokuapp.com/maven-central/com.github.dtaniwaki/akka-pusher_2.11
[coverage-image]: http://codecov.io/github/dtaniwaki/akka-pusher/coverage.svg
[coverage-link]:  http://codecov.io/github/dtaniwaki/akka-pusher

