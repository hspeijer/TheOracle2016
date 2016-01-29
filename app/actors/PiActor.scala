package actors

import actors.OracleActor.{Button, Oracle, ButtonLight, PlayMedia}
import akka.actor.{Props, ActorLogging, Actor, ActorRef}
import akka.event.LoggingReceive
import com.pi4j.io.gpio._
import com.pi4j.io.gpio.event.{GpioPinDigitalStateChangeEvent, GpioPinListenerDigital}
import play.api.libs.json.{JsValue, Json}

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
  var earth : GpioPinDigitalOutput = null
  var fire : GpioPinDigitalOutput = null
  var water : GpioPinDigitalOutput = null
  var air : GpioPinDigitalOutput = null
  var aether : GpioPinDigitalOutput = null

  var input : GpioPinDigitalInput = null

  override def preStart() = {

    try {
      gpio = GpioFactory.getInstance()
      earth = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_08, "Earth", PinState.LOW)
      earth.setShutdownOptions(true, PinState.LOW)

      fire = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_09, "Fire", PinState.LOW)
      fire.setShutdownOptions(true, PinState.LOW)

      water = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_07, "Water", PinState.LOW)
      water.setShutdownOptions(true, PinState.LOW)

      air = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00, "Air", PinState.LOW)
      air.setShutdownOptions(true, PinState.LOW)

      aether = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02, "Aether", PinState.LOW)
      aether.setShutdownOptions(true, PinState.LOW)

      input = gpio.provisionDigitalInputPin(RaspiPin.GPIO_03)

      class InputListener extends GpioPinListenerDigital {
        override def handleGpioPinDigitalStateChangeEvent(event: GpioPinDigitalStateChangeEvent) = {
          BoardActor() ! OracleActor.ButtonSelect(Button.Earth)
        }
      }
      input.addListener(new InputListener())

      BoardActor() ! Subscribe
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
    case lights:ButtonLight => {
      earth.setState(lights.earth)
      fire.setState(lights.fire)
      water.setState(lights.water)
      air.setState(lights.air)
      aether.setState(lights.aether)
    }
    case other => log.error("unhandled: " + other)
  }
}

object PiActor {
}
