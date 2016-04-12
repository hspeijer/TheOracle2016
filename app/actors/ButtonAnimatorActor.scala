package actors

import actors.ButtonAnimatorActor.{Animate, Tick, Stop}
import akka.actor.{Cancellable, ActorLogging, Actor, Props}
import akka.event.LoggingReceive
import model.Button.Button
import model._
import play.libs.Akka
import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Copyright 2014 mindsteps BV
 *
 * User: Hans Speijer
 * Date: 10/02/16
 * Time: 16:44
 */
class ButtonAnimatorActor extends Actor with ActorLogging {

  val fps = 24
  var pulseSchedule : Cancellable = null
  var currentAnimation : Animation = new ColourCycle()
  var currentFrame : Seq[Light] = emptyFrame()
  var currentSensors : Set[Button] = Set[Button]()
  var frameCount = 0

  def emptyFrame() = List(new Light(), new Light(), new Light(), new Light(), new Light()).toSeq

  abstract class Animation {
     def renderFrame(frame : Long, currentFrame: Seq[Light]) : Seq[Light]
  }

  class SensorTriggerAnimation extends Animation {

    currentFrame = emptyFrame()

    val sensorSelect = mutable.Map(
      Button.Fire -> 0,
      Button.Aether -> 0,
      Button.Earth -> 0,
      Button.Air -> 0,
      Button.Water -> 0
    )

    override def renderFrame(frame: Long, currentFrame: Seq[Light]): Seq[Light] = {

      for(item <- sensorSelect) {
        if(currentSensors.contains(item._1)) {
          sensorSelect.update(item._1, minMax(0, 100, item._2 + 2))
        } else {
          sensorSelect.update(item._1, minMax(0, 100, item._2 - 3))
        }
      }

      if(numValues() == 1 && (sensorSelect.getOrElse(maxSelect(),0) >= 100)) {
        println("Trigger! " + maxSelect())
        BoardActor() ! SensorTrigger(maxSelect())
        for(item <- sensorSelect) {
          sensorSelect.update(item._1, 0)
        }
      }

      val result = mutable.MutableList[Light]()
      var offset = 0
      for(element <- ElementColours.colours) {
        if(sensorSelect(element._1) > 0) {
          result += Light(ColourRGB(
            (sensorSelect(element._1) / 100.0 * element._2.red).toShort,
            (sensorSelect(element._1) / 100.0 * element._2.green).toShort,
            (sensorSelect(element._1) / 100.0 * element._2.blue).toShort
          ), 255)
        } else {
          val brightness = ((math.sin((0.03 * (frame - 5 * offset))) + 1.0) * 127).toShort
          result += Light(ColourRGB(
            (brightness / 2500.0 * element._2.red).toShort,
            (brightness / 2500.0 * element._2.green).toShort,
            (brightness / 2500.0 * element._2.blue).toShort
          ), 255)

        }

        offset += 1
      }

      result
    }

    def minMax(min: Int, max: Int, value: Int) = {
      if(value < min) {
        min
      } else if(value > max) {
        max
      } else {
        value
      }
    }

    def maxSelect() : Button = {
      var result : (Button.Button, Int) = null

      for(compare <- sensorSelect.iterator) {
        if(result == null) {
          result = compare
        } else if(compare._2 > result._2) {
          result = compare
        }
      }
      result._1
    }

    def numValues() : Int = {
      var result = 0
      for(triggerValue <- sensorSelect.values) {
        if(triggerValue > 0) {
          result += 1
        }
      }
      result
    }
  }

  class ColourCycle extends Animation {
    override def renderFrame(frame: Long, currentFrame: Seq[Light]): Seq[Light] = {
      val result = mutable.MutableList[Light]()

      def wave(frame: Long, frequency : Double, offset: Int) : Short = {
        //        setLightColour("g", scope("light"), cast(multiply(add(sin(multiply(constant(0.015), add(scope("frame"), multiply(scope("light"), constant(4))))),constant(1)), constant(127)))),
        ((math.sin((frequency * (frame + offset))) + 1.0) * 127).toShort
      }

      var offset = 0
      for(light <- currentFrame) {
        result += Light(ColourRGB(
          wave(frame, 0.03, offset * 5),
          wave(frame, 0.032, offset * 5),
          wave(frame, 0.027, offset * 5)
        ), 255)

        offset += 1
      }

      result
    }
  }

  class Pulse(button: Button) extends Animation {

    val buttons = List(Button.Fire, Button.Aether, Button.Earth, Button.Air, Button.Water)
    val buttonIndex = buttons.indexOf(button)

    override def renderFrame(frame: Long, currentFrame: Seq[Light]): Seq[Light] = {
      val result = mutable.MutableList[Light]()

      def wave(frame: Long, frequency : Double, offset: Int) : Short = {
        (32 + (math.sin((frequency * (frame + offset))) + 1.0) * 112).toShort
      }

      for(i <- 0 until 5) {
        if(i == buttonIndex) {
          val targetColour = ElementColours.colours(buttonIndex)_2
          var brightness = wave(frame, 0.05, 0)
           result += new Light(ColourRGB(
             (brightness/256.0 * targetColour.red).toShort ,
             (brightness/256.0 * targetColour.green).toShort ,
             (brightness/256.0 * targetColour.blue).toShort
           ))
        } else {
          result += new Light()
        }
      }

      result
    }
  }

  def tick(): Unit = {
//    if(!currentAnimator.finished) {
//      currentAnimator.pulse()
//      val oldState = state
//      state = new ButtonLight(currentAnimator.value())
//      if (oldState.toByte() != state.toByte()) {
//        BoardActor() ! state
//      }
//    } else {
//      animatorIndex = animatorIndex + 1
//      currentAnimator.reset()
//      println("AnimatorIndex" + animatorIndex + " " + animatorIndex % (animation.length - 1))
//      currentAnimator = animation(animatorIndex % (animation.length - 1))
//    }
    currentFrame = currentAnimation.renderFrame(frameCount, currentFrame)
    frameCount += 1

//    println("Frame " + currentFrame + " " + frameCount)

    BoardActor() ! LightState(currentFrame)
  }

  def receive = LoggingReceive {
    case animation: Animate => {
      BoardActor() ! Message("0", "Starting animation " + animation.name)
      if(pulseSchedule != null) {
        pulseSchedule.cancel()
      }

      animation.name match {
        case "Colours" => currentAnimation = new ColourCycle()
        case "SensorSelect" => currentAnimation = new SensorTriggerAnimation()
        case "Pulse" => currentAnimation = new Pulse(animation.button)
      }

      pulseSchedule = Akka.system().scheduler.schedule(0 seconds, 1000/fps millisecond, ButtonAnimatorActor(), Tick())
    }
    case _: Stop => {
      if(pulseSchedule != null && !pulseSchedule.isCancelled) pulseSchedule.cancel()
      BoardActor() ! LightState
    }
    case _: Tick => tick()
    case sensors:SensorSelect => {
      currentSensors = sensors.sensors
    }
    case other => log.info("unhandled: " + other)
  }


}


object ButtonAnimatorActor {
  lazy val animator = Akka.system().actorOf(Props[ButtonAnimatorActor])
  def apply() = animator

  case class Animate(name: String, button: Button)
  case class Stop()
  case class Tick()
}
