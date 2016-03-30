package actors

import akka.actor.{ActorLogging, Actor}
import akka.event.LoggingReceive
import model.PlayMedia

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
  val command = "omxplayer --win -500,-280,1800,1300 ./public/mp4/"

  override def preStart() = {
    BoardActor() ! Subscribe
  }

  def receive = LoggingReceive {
    case media:PlayMedia => {
      val commandStr = command + media.name + ".mp4"

      log.info("Exec " + commandStr)

      try {
        commandStr !
      } catch {
        case ex: Throwable => println(ex.getMessage)
      }

    }
    case other => log.info("unhandled: " + other)
  }

}
