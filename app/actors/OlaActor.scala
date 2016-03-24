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
  val UNIVERSE_ID: Short = 0

  var client: OlaClient = null
  var lights: Array[OlaLight] = Array(new OlaLight(0), new OlaLight(1), new OlaLight(2), new OlaLight(3), new OlaLight(4))

  override def preStart() = {
    client = new OlaClient()
    client.sendDmx(0, Array[Short](127, 0, 255, 255).padTo(512, 0.toShort))

    BoardActor() ! Subscribe
  }

  def receive = LoggingReceive {
    case lightsMessage: ButtonLight => {

      clearAll()

      if (lightsMessage.air) {
        lights(0).setARGB(255, 0, 127, 255)
        //client.sendDmx(0, Array[Short](127,0,64,127).padTo(512, 0.toShort))
      }
      if (lightsMessage.earth) {
        lights(1).setARGB(255, 0, 255, 0)
        //client.sendDmx(0, Array[Short](127,0,127,0).padTo(512, 0.toShort))
      }
      if (lightsMessage.fire) {
        lights(2).setARGB(255, 255, 0, 0)
        //client.sendDmx(0, Array[Short](127,127,0,0).padTo(512, 0.toShort))
      }
      if (lightsMessage.water) {
        lights(3).setARGB(255, 0, 0, 255)
        //client.sendDmx(0, Array[Short](127,0,0,127).padTo(512, 0.toShort))
      }
      if (lightsMessage.aether) {
        //client.sendDmx(0, Array[Short](127,127,127,127).padTo(512, 0.toShort))
        lights(4).setARGB(255, 255, 255, 255)
      }
      sendDmx()
    }
  }

  def clearAll() = {
    for (light <- lights) {
      light.clear()
    }
  }

  def sendDmx(): Unit = {
    var frame = Array[Short]().padTo(512, 0.toShort)
    for (light <- lights) {
      light.addToFrame(frame)
    }
    client.sendDmx(UNIVERSE_ID, frame)
  }

}

class ARGB(var a: Short = 0,
           var r: Short = 0,
           var g: Short = 0,
           var b: Short = 0) {

  def this() = {
    this(0, 0, 0, 0)
  }

  def toArray() = {
    Array[Short](a, r, g, b)
  }
}

class OlaLight(id: Int, argb: ARGB) {

  def this(id: Int) = {
    this(id, new ARGB(0, 0, 0, 0))
  }

  def clear() = {
    setARGB(0, 0, 0, 0)
  }

  def setARGB(a: Short, r: Short, g: Short, b: Short) = {
    argb.a = a
    argb.r = r
    argb.g = g
    argb.b = b
  }

  def addToFrame(frameData: Array[Short]) = {
    argb.toArray().copyToArray(frameData, id * 4)
  }
}

class FadingLight(id: Short, argb: ARGB) extends OlaLight(id, argb) {
  var currentStep = 0
  var steps = 0
  var startARGB = new ARGB()
  var endARGB = new ARGB()
}

}