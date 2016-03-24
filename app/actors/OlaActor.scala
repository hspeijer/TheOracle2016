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
  var lights: Array[OlaLight] = Array(new OlaLight(), new OlaLight(), new OlaLight(), new OlaLight(), new OlaLight())

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
        // }
        if (lightsMessage.water) {
          lights(3).setARGB(255, 255, 0, 0)
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
        var frame = Array[Short]().padTo(512, 0)
        for (light <- lights) {
          light.addToFrame(frame)
        }
        client.sendDmx(UNIVERSE_ID, frame)
      }

  }


  class OlaLight(
                  var id: Short = 0,
                  var a: Short = 0,
                  var r: Short = 0,
                  var g: Short = 0,
                  var b: Short = 0
                  ) {

    def this() = {
      this(0, 0, 0, 0)
    }

    def clear() = {
      setARGB(0, 0, 0, 0)
    }

    def setARGB(a: Short, r: Short, g: Short, b: Short) = {
      this.a = a
      this.r = r
      this.g = g
      this.b = b
    }

    def addToFrame(frameData: Array[Short]) = {
      frameData(id * 4) = a
      frameData(id * 4 + 1) = r
      frameData(id * 4 + 2) = g
      frameData(id * 4 + 3) = b
    }
  }
}