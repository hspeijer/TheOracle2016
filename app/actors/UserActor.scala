package actors

import actors.OracleActor.{Button, ButtonSelect, ButtonLight, PlayMedia}
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.event.LoggingReceive
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import akka.actor.ActorRef
import akka.actor.Props
import scala.xml.Utility


class UserActor(uid: String, board: ActorRef, out: ActorRef) extends Actor with ActorLogging {

  override def preStart() = {
    BoardActor() ! Subscribe
  }

  def receive = LoggingReceive {
    case Message(muid, s) if sender == board => {
      println("User receive " + s)
      val js = Json.obj("type" -> "message", "uid" -> muid, "msg" -> s)
      out ! js
    }
    case media:PlayMedia => {
      println("User Received play media " + media.name)
      val js = Json.obj("type" -> "media", "name" -> media.name, "msg" -> ("Playing " + media.name))
      out ! js
    }
    case lights:ButtonLight => {
      val js = Json.obj("type" -> "lights", "earth" -> lights.earth, "air" -> lights.air, "water" -> lights.water, "fire" -> lights.fire, "aether" -> lights.aether)
      out ! js
    }
    case js: JsValue => {
      (js \ "msg").validate[String] map { Utility.escape(_) }  map { board ! Message(uid, _ ) }
//      if(js \ "type" == "button-click") {
//        js \ "button" match {
//          case "air" => BoardActor() ! ButtonSelect(Button.Air)
//          case "fire" => BoardActor() ! ButtonSelect(Button.Fire)
//          case "earth" => BoardActor() ! ButtonSelect(Button.Earth)
//          case "water" => BoardActor() ! ButtonSelect(Button.Water)
//          case "aether" => BoardActor() ! ButtonSelect(Button.Aether)
//        }
//      }
    }
    case other => log.error("unhandled: " + other)
  }
}

object UserActor {
  def props(uid: String)(out: ActorRef) = Props(new UserActor(uid, BoardActor(), out))
}