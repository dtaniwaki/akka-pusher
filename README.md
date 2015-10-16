# akka-pusher

[![Circle CI][circle-ci-image]][circle-ci-link]

Pusher Client under Akka's actor context.

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

## Contributing

1. Fork it
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Add some feature'`)
4. Push to the branch (`git push origin my-new-feature`)
5. Create new [Pull Request](../../pull/new/master)

## Copyright

Copyright (c) 2015 Daisuke Taniwaki. See [LICENSE](LICENSE) for details.

[circle-ci-image]:  https://circleci.com/gh/dtaniwaki/akka-pusher/tree/master.png?style=badge
[circle-ci-link]:   https://circleci.com/gh/dtaniwaki/akka-pusher/tree/master
