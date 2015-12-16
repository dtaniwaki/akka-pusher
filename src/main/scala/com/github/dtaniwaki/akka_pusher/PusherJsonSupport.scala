package com.github.dtaniwaki.akka_pusher

import spray.json.DefaultJsonProtocol

// Just prioritize the implicit conversions
trait PusherJsonSupport extends DefaultJsonProtocol {
}
