package actors

import akka.actor.{ActorLogging, Actor}
import akka.event.LoggingReceive
import model.{MediaComplete, PlayMedia}
import play.api.Play
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

  val command = Play.current.configuration.getString("player.command").getOrElse("omxplayer")
  val mediaLocation = Play.current.configuration.getString("media.location").getOrElse("./")

  override def preStart() = {
    BoardActor() ! Subscribe
  }

  def receive = LoggingReceive {
    case media:PlayMedia => {
      val commandStr = command + " " + mediaLocation + media.media.name + ".m4v"

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
