package actors

import akka.actor.{ActorLogging, Actor}
import akka.event.LoggingReceive
import model.{LightState, Light}
import ola.OlaClient

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

    if(client != null) {
      client.sendDmx(UNIVERSE_ID, frame)
    }
  }
}

