package actors

import actors.ButtonAnimatorActor.{Animate, Tick, Pulse, Stop}
import akka.actor.{Cancellable, ActorLogging, Actor, Props}
import akka.event.LoggingReceive
import model.{LightState, ColourRGB, Light}
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

  val fps = 5

  abstract class Animation {
     def renderFrame(frame : Long, currentFrame: Seq[Light]) : Seq[Light]
  }

  class FadeIn(lightIndex : Short, startFrame: Long, duration: Short, colour : ColourRGB) extends Animation {
    override def renderFrame(frame: Long, currentFrame: Seq[Light]): Seq[Light] = {
      currentFrame
    }
  }

  class FadeOut(lightIndex : Short, startFrame: Long, duration: Short) extends Animation {
    override def renderFrame(frame: Long, currentFrame: Seq[Light]): Seq[Light] = {
      currentFrame
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
          wave(frame, 0.15, offset * 5),
          wave(frame, 0.14, offset * 5),
          wave(frame, 0.17, offset * 5)
        ), 255)

        offset += 1
      }

      result
    }
  }

  var pulseSchedule : Cancellable = null


  var currentAnimation = new ColourCycle()
  var currentFrame = (new Array[Light](5)).toSeq
  var frameCount = 0

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
    case _: Animate => {
      println("Starting animation")
      if(pulseSchedule != null) {
        pulseSchedule.cancel()
      }
      pulseSchedule = Akka.system().scheduler.schedule(0 seconds, 1000/fps millisecond, ButtonAnimatorActor(), Tick())
    }
    case _: Stop => {
      if(pulseSchedule != null && !pulseSchedule.isCancelled) pulseSchedule.cancel()
      BoardActor() ! LightState
    }
    case _: Tick => tick()
    case other => log.info("unhandled: " + other)
  }


}


object ButtonAnimatorActor {
  lazy val animator = Akka.system().actorOf(Props[ButtonAnimatorActor])
  def apply() = animator

  case class Animate()
  case class Stop()
  case class Pulse()
  case class Tick()
}
