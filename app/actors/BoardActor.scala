package actors

import actors.OracleActor.{ButtonLight, PlayMedia}
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.event.LoggingReceive
import akka.actor.ActorRef
import akka.actor.Terminated
import play.api.libs.json.Json
import play.libs.Akka
import akka.actor.Props

class BoardActor extends Actor with ActorLogging {
  var users = Set[ActorRef]()

  def receive = LoggingReceive {
    case m:Message => {
      println("Message received:" + m + " " + this)
      users map { _ ! m}
    }
    case media:PlayMedia => {
      println("Playing media " + media.name)
      users map { _ ! media}
    }
    case button:ButtonState => {
      users map { _ ! button}
    }
    case pattern:ButtonLight => {
      users map { _ ! pattern}
    }
    case Subscribe => {
      users += sender
      context watch sender
    }
    case Terminated(user) => users -= user
  }
}

object BoardActor {
  lazy val board = Akka.system().actorOf(Props[BoardActor])
  def apply() = board
}

case class Message(uuid: String, s: String)
object Subscribe
