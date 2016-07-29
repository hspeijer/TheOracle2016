package actors

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.event.LoggingReceive
import model.Button
import model.Button._
import model._
import play.api.libs.json.{JsSuccess, JsError, JsValue, Json}
import akka.actor.ActorRef
import akka.actor.Props
import scala.collection.mutable
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
      println("User Received play media " + media.media.name)
      val js = Json.obj("type" -> "media", "name" -> media.media.name, "msg" -> ("Playing " + media.media.name))
      out ! js
    }
    case lights:LightState => {
      val js = Json.obj("type" -> "lights", "fire" -> lights.lights(0).toJson(), "aether" -> lights.lights(1).toJson(), "earth" -> lights.lights(2).toJson(), "air" -> lights.lights(3).toJson(), "water" -> lights.lights(4).toJson())
      out ! js
    }
    case sensor:SensorSelect => {
      val js = Json.obj("type" -> "sensors-state" , "sensors" -> Json.obj(
        "fire" -> sensor.sensors.contains(Button.Fire),
        "aether" -> sensor.sensors.contains(Button.Aether),
        "earth" -> sensor.sensors.contains(Button.Earth),
        "air" -> sensor.sensors.contains(Button.Air),
        "water" -> sensor.sensors.contains(Button.Water)
      ))
      out ! js
    }
    case smoke:DoSmoke => {
      val js = Json.obj("type" -> "smoke", "duration" -> smoke.duration, "intensity" -> smoke.intensity , "msg" -> ("Smoke it! "))
      out ! js
    }
    case js: JsValue => {
//      (js \ "msg").validate[String] map { Utility.escape(_) }  map { board ! Message(uid, _ ) }
      if(((js \ "type").as[String]).equals("button-click")) {
        (js \ "button").as[String] match {
          case "air" => BoardActor() ! SensorSelect(mutable.SortedSet[Button](Button.Air).toSet)
          case "fire" => BoardActor() ! SensorSelect(mutable.SortedSet[Button](Button.Fire).toSet)
          case "earth" => BoardActor() ! SensorSelect(mutable.SortedSet[Button](Button.Earth).toSet)
          case "water" => BoardActor() ! SensorSelect(mutable.SortedSet[Button](Button.Water).toSet)
          case "aether" => BoardActor() ! SensorSelect(mutable.SortedSet[Button](Button.Aether).toSet)
        }
        println("button-click")
      } else if (((js \ "type").as[String]).equals("sensor-state")) {
        var sensorState = mutable.SortedSet[Button]()
        (js \ "sensors" \ "fire").validate[Boolean] match {
          case s: JsSuccess[String] => sensorState += Button.Fire
          case e: JsError => {}
        }
        (js \ "sensors" \ "aether").validate[Boolean] match {
          case s: JsSuccess[String] => sensorState += Button.Aether
          case e: JsError => {}
        }
        (js \ "sensors" \ "earth").validate[Boolean] match {
          case s: JsSuccess[String] => sensorState += Button.Earth
          case e: JsError => {}
        }
        (js \ "sensors" \ "air").validate[Boolean] match {
          case s: JsSuccess[String] => sensorState += Button.Air
          case e: JsError => {}
        }
        (js \ "sensors" \ "water").validate[Boolean] match {
          case s: JsSuccess[String] => sensorState += Button.Water
          case e: JsError => {}
        }
        board ! SensorSelect(sensorState.toSet)
      } else {
        (js \ "msg").validate[String] map { Utility.escape(_) }  map { board ! Message(uid, _ )}
      }
    }
    case other => {}
  }
}

object UserActor {
  def props(uid: String)(out: ActorRef) = Props(new UserActor(uid, BoardActor(), out))
}