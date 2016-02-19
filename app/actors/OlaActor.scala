package actors

import actors.OracleActor.ButtonLight
import akka.actor.{ActorLogging, Actor}
import akka.event.LoggingReceive
import ola.OlaClient

/**
 * Copyright 2014 mindsteps BV
 *
 * User: Hans Speijer
 * Date: 19/02/16
 * Time: 14:15
 */
class OlaActor extends Actor with ActorLogging {

  var client : OlaClient = null

  override def preStart() = {
    client = new OlaClient();
    client.sendDmx(0, Array[Short](254,255,255,255))

    BoardActor() ! Subscribe
  }

  def receive = LoggingReceive {
    case lights: ButtonLight => {
      if(lights.air) { client.sendDmx(0, Array[Short](254,0,234,254)) }
      if(lights.earth) { client.sendDmx(0, Array[Short](254,0,254,0))}
      if(lights.fire) { client.sendDmx(0, Array[Short](254,254,0,0))}
      if(lights.water) { client.sendDmx(0, Array[Short](254,0,0,254))}
      if(lights.aether) { client.sendDmx(0, Array[Short](254,254,254,254))}
    }
  }
}
