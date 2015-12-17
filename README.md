# akka-pusher

[![Maven Central][maven-image]][maven-link]
[![Coverage][coverage-image]][coverage-link]
[![CI][ci-image]][ci-link]
[![License: MIT](http://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

[Pusher](https://pusher.com/) Client under Akka's actor context.

The working sample with Play Framework is available <a href="https://github.com/dtaniwaki/akka-pusher-play-app" target="_blank">here</a>.

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
val channels: Future[Try[ChannelMap]] = pusher.channels("presence-my_", Seq(PusherChannelsAttributes.userCount))
```

#### channel

```scala
val channel: Future[Try[Channel]] = pusher.channel("presence-my_channel", Seq(PusherChannelAttributes.userCount))
```

#### users

```scala
val users: Future[Try[UserList]] = pusher.users("presence-my_channel")
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

If the `batchTrigger` setting of `PusherActor` is `true`,

```scala
pusherActor ! TriggerMessage(channel, event, body.toJson, socketId)
```

The trigger will be executed in batch in 1000 milliseconds (default).

#### ChannelMessage

```scala
(pusherActor ask ChannelMessage("presence-my_channel", Seq(PusherChannelAttributes.userCount))).map {
  case Success(res: PusherModels.Channel) => println(res)
  case Failure(e) => throw e
}
```

#### ChannelsMessage

```scala
(pusherActor ask ChannelsMessage("presence-my_", Seq(PusherChannelsAttributes.userCount))).map {
  case Success(res: ChannelMap) =>
    println(res)
  case Failure(e) => throw e
}
```

#### UserMessage

```scala
(pusherActor ask UsersMessage("presence-my_channel")).map {
  case Success(res: UserList) =>
    println(res)
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

PusherClient use `pusher` scope in `application.conf` parsed by [typesafe config](https://github.com/typesafehub/config).

```
pusher {
  appId=${?PUSHER_APP_ID}
  key=${?PUHSER_API_KEY}
  secret=${?PUSHER_API_SECRET}
  batchTrigger=true
  batchInterval=1000
}
```

Here, you can replace the variables or set them as environment variables.

Or, you can directly set the config by the costructor argument.

```scala
val pusher = new PusherClient(ConfigFactory.parseString("""pusher: {appId: "app0", key: "key0", secret: "secret0"}"""))
```

### PusherClient Configuration

| key            | type     | description |
|---------------:|:---------|:------------|
| `appId`        | `String` | Your pusher app ID. |
| `key`          | `String` | Your pusher app key. |
| `secret`       | `String` | Your pusher app secret. |
| `ssl` | `Boolean` (default: false) | Encrypt API request with SSL |

### PusherActor Configuration

| key            | type     | description |
|---------------:|:---------|:------------|
| `batchTrigger` | `Boolean` (default: false) | Flag to enable batch trigger requests. The batch size is 100 as pusher limits it. |
| `batchInterval` | `Int` (default: 1000) | Milliseconds to make batch trigger requests. |

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

[ci-image]:  https://travis-ci.org/dtaniwaki/akka-pusher.svg?branch=master
[ci-link]:   https://travis-ci.org/dtaniwaki/akka-pusher?branch=master
[maven-image]:  https://maven-badges.herokuapp.com/maven-central/com.github.dtaniwaki/akka-pusher_2.11/badge.svg?style=plastic
[maven-link]:   https://maven-badges.herokuapp.com/maven-central/com.github.dtaniwaki/akka-pusher_2.11
[coverage-image]: http://codecov.io/github/dtaniwaki/akka-pusher/coverage.svg?branch=master
[coverage-link]:  http://codecov.io/github/dtaniwaki/akka-pusher?branch=master

