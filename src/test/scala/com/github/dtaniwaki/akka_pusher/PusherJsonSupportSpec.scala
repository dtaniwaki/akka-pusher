package com.github.dtaniwaki.akka_pusher

import com.github.dtaniwaki.akka_pusher.PusherModels.{ User, Channel }
import org.specs2.mutable.Specification
import org.specs2.specification.process.RandomSequentialExecution
import spray.json._

class PusherJsonSupportSpec extends Specification
    with SpecHelper
    with RandomSequentialExecution
    with PusherJsonSupport {

  "UserListJsonFormat" should {
    "with users" in {
      "read from json object" in {
        val users = List[User](User("123"), User("234"))
        """{"users": [{"id": "123"}, {"id": "234"}]}""".parseJson.convertTo[List[User]] === users
      }
      "write to json object" in {
        val users = List[User](User("123"), User("234"))
        users.toJson === """{"users": [{"id": "123"}, {"id": "234"}]}""".parseJson
      }
    }
    "without users" in {
      "read from json object" in {
        val users = List[User]()
        """{"users": []}""".parseJson.convertTo[List[User]] === users
      }
      "write to json object" in {
        val users = List[User]()
        users.toJson === """{"users": []}""".parseJson
      }
    }
  }
}
