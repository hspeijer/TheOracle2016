package actors

import actors.OracleActor.{PlayMedia, ButtonLight}
import akka.actor.{ActorLogging, Actor}
import akka.event.LoggingReceive

import sys.process._

/**
 * Copyright 2014 mindsteps BV
 *
 * User: Hans Speijer
 * Date: 03/02/16
 * Time: 18:12
 */
class VideoPlayerActor extends Actor with ActorLogging {
//  val command = "open /Applications/VLC.app ./public/mp4/"
  val command = "omxplayer --win -500,-280,1800,1300 ./public/mpeg2/"

  override def preStart() = {
    BoardActor() ! Subscribe
  }

  def receive = LoggingReceive {
    case media:PlayMedia => {
      val commandStr = command + media.name + ".mpg"

      log.info("Exec " + commandStr)

      commandStr !

    }
    case other => log.info("unhandled: " + other)
  }

}
