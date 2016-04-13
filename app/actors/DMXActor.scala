package actors

import akka.actor.{ActorLogging, Actor}
import akka.event.LoggingReceive
import model.{DoSmoke, LightState, Light}
import ola.OlaClient
import play.libs.Akka

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Copyright 2014 mindsteps BV
 *
 * User: Hans Speijer
 * Date: 19/02/16
 * Time: 14:15
 */
class DMXActor extends Actor with ActorLogging {
  val UNIVERSE_ID: Short = 0

  var client: OlaClient = null
  var smoking : Short = 0

  override def preStart() = {
    try {
      client = new OlaClient()
    } catch {
      case ex: Throwable => println("Could not init DMX")
    }

    BoardActor() ! Subscribe
  }

  def receive = LoggingReceive {
    case lights: LightState => {
      sendDmx(lights.lights)
    }
    case smoke: DoSmoke => {
      println("Smoke! " + smoke)
      smoking = smoke.intensity

      Akka.system().scheduler.scheduleOnce(smoke.duration millisecond, new Runnable {
        override def run(): Unit = {smoking = 0}
      })
    }
  }

  def sendDmx(lights : Seq[Light]) = {
    var frame = new Array[Short](512)
    var offset = 0

     for(light <- lights) {
       frame(offset) = light.effect
       frame(offset + 1) = light.colour.red
       frame(offset + 2) = light.colour.green
       frame(offset + 3) = light.colour.blue

       offset += 4
     }

    frame(offset) = smoking

    if(client != null) {
      client.sendDmx(UNIVERSE_ID, frame)
    }
  }
}

