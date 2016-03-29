package actors

import actors.OracleActor.Button
import actors.OracleActor.Button.Button
import actors.OracleActor.{Button, Oracle, ButtonLight, PlayMedia}
import akka.actor.{Props, ActorLogging, Actor, ActorRef}
import akka.event.LoggingReceive
import com.pi4j.io.gpio._
import com.pi4j.io.gpio.event.{GpioPinDigitalStateChangeEvent, GpioPinListenerDigital}
import play.api.libs.json.{JsValue, Json}

import scala.collection.mutable
import scala.xml.Utility

/**
 * Copyright 2014 mindsteps BV
 *
 * User: Hans Speijer
 * Date: 29/01/16
 * Time: 21:34
 */
class PiActor extends Actor with ActorLogging {

  var gpio : GpioController = null
  var earth : GpioPinDigitalInput = null
  var fire : GpioPinDigitalInput = null
  var water : GpioPinDigitalInput = null
  var air : GpioPinDigitalInput = null
  var aether : GpioPinDigitalInput = null

  var sensorState = mutable.SortedSet[Button]()

  override def preStart() = {

    try {
      gpio = GpioFactory.getInstance()

      fire = gpio.provisionDigitalInputPin(RaspiPin.GPIO_08)
      fire.setDebounce(500)
      aether = gpio.provisionDigitalInputPin(RaspiPin.GPIO_09)
      aether.setDebounce(500)
      earth = gpio.provisionDigitalInputPin(RaspiPin.GPIO_07)
      earth.setDebounce(500)
      air = gpio.provisionDigitalInputPin(RaspiPin.GPIO_00)
      air.setDebounce(500)
      water = gpio.provisionDigitalInputPin(RaspiPin.GPIO_02)
      water.setDebounce(500)

      class SensorListener(button : Button) extends GpioPinListenerDigital {
        override def handleGpioPinDigitalStateChangeEvent(event: GpioPinDigitalStateChangeEvent) = {
          if(event.getState.isLow) {
            BoardActor() ! OracleActor.ButtonSelect(button)
            sensorState += button
          } else {
            sensorState -= button
          }
          println("Sensors: " + sensorState)
        }
      }
      fire.addListener(new SensorListener(Button.Fire))
      aether.addListener(new SensorListener(Button.Aether))
      earth.addListener(new SensorListener(Button.Earth))
      air.addListener(new SensorListener(Button.Air))
      water.addListener(new SensorListener(Button.Water))

    } catch {
      case error: UnsatisfiedLinkError => println("No GPIO lib loaded")
    }
  }

  override def postStop() = {
    if(gpio != null) {
      gpio.shutdown()
    }
  }
  
  def receive = LoggingReceive {
    case other => log.info("unhandled: " + other)
  }
}

object PiActor {
}
