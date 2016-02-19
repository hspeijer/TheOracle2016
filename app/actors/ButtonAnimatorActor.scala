package actors

import actors.ButtonAnimatorActor.{Animate, Tick, Pulse, Stop}
import actors.OracleActor.ButtonLight
import akka.actor.{Cancellable, ActorLogging, Actor, Props}
import akka.event.LoggingReceive
import play.libs.Akka
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

  var state = new ButtonLight(0)
  var pulseSchedule : Cancellable = null

  abstract class Animator() {
    var pulseCount : Int = 0

    def pulse() : Unit = pulseCount += 1
    def finished() : Boolean
    def value() : Byte
    def reset() = pulseCount = 0
  }

  class Flash(pulseOn: Int, pulseOf: Int) extends Animator{
    def value() : Byte = if(pulseCount <= pulseOn) 0x1f else 0x00
    def finished() : Boolean = pulseCount > pulseOn + pulseOf
  }

  class Walker(pulseHold: Int, leftToRight: Boolean) extends Animator {
    var state = if(leftToRight) 16 else 1

    override def pulse() : Unit = {
      if (pulseCount % pulseHold == 0 && pulseCount > 0) {
        if(leftToRight) {
          state = state >> 1
        } else {
          state = state << 1
        }
        //println("animator" + state.toBinaryString)
      }
      pulseCount += 1
    }
    def value() : Byte = state.toByte
    def finished() : Boolean = pulseCount >= pulseHold * 5
    override def reset() = {
      state = if(leftToRight) 16 else 1
      pulseCount = 0;
    }
  }

  def animate() = {
    println("Starting animation!")

    animation.foreach(animator => {
      while(!animator.finished()) {
        animator.pulse()
        val oldState = state
        state = new ButtonLight(animator.value())
        if (oldState.toByte() != state.toByte()) {
          BoardActor() ! state
        }
        Thread.sleep(100)
      }
    })
  }

  var animation = List(
    new Flash(12, 6),
    new Flash(12, 6),
    new Walker(4, true),
    new Walker(4, false),
    new Walker(2, true),
    new Walker(2, false),
    new Walker(2, true),
    new Walker(2, true),
    new Walker(2, true),
    new Walker(2, true),
    new Walker(2, true)
  )

  var currentAnimator : Animator = animation(0)
  var animatorIndex = 0

  def tick(): Unit = {
    if(!currentAnimator.finished) {
      currentAnimator.pulse()
      val oldState = state
      state = new ButtonLight(currentAnimator.value())
      if (oldState.toByte() != state.toByte()) {
        BoardActor() ! state
      }
    } else {
      animatorIndex = animatorIndex + 1
      currentAnimator.reset()
      println("AnimatorIndex" + animatorIndex + " " + animatorIndex % (animation.length - 1))
      currentAnimator = animation(animatorIndex % (animation.length - 1))
    }
  }

  def receive = LoggingReceive {
    case _: Animate => {
      println("Starting animation")
      animatorIndex = 0
      if(pulseSchedule != null) {
        pulseSchedule.cancel()
      }
      pulseSchedule = Akka.system().scheduler.schedule(0 seconds, 100 millisecond, ButtonAnimatorActor(), Tick())
    }
    case _: Stop => {
      if(pulseSchedule != null && !pulseSchedule.isCancelled) pulseSchedule.cancel()
      BoardActor() ! new ButtonLight(0)
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
