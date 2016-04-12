package actors

import akka.actor.{ActorLogging, Actor}
import akka.event.LoggingReceive
import model.{MediaComplete, PlayMedia}
import play.libs.Akka
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

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
  val command = "omxplayer /home/pi/TheOracleMedia/h264/"

  override def preStart() = {
    BoardActor() ! Subscribe
  }

  def receive = LoggingReceive {
    case media:PlayMedia => {
      val commandStr = command + media.media.name + ".m4v"

      Akka.system().scheduler.scheduleOnce(media.media.duration seconds, BoardActor(), MediaComplete(media.media))
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
