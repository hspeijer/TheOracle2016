package actors

import java.io._

import akka.actor.{Props, ActorLogging, Actor, ActorRef}
import akka.event.LoggingReceive
import gnu.io.{SerialPort, CommPortIdentifier}
import model.{SensorSelect, Button}
import model.Button._

import scala.collection.mutable

/**
 * Copyright 2014 mindsteps BV
 *
 * User: Hans Speijer
 * Date: 28/07/16
 * Time: 03:03
 */

abstract class ProtocolHandler {
  def getSerialReader(in: InputStream) : Runnable
}

class BinaryProtocol extends ProtocolHandler {
  def getSerialReader(in: InputStream) : Runnable = return new SerialReader(in)

  println("Init Binary Protocol")

  for(i <- 0 until ArduinoActor.sensors.length) {
    ArduinoActor.sensors(i) = new SensorState()
  }

  /***/
  class SerialReader(in: InputStream) extends Runnable {
    def run: Unit = {

      val reader = new BufferedReader( new InputStreamReader(in))
      var inputLine = ""
      var measureIndex = 0

      while ((inputLine = reader.readLine()) != null ) {
        measureIndex += 1
//        println("Received:" + inputLine)
        val values = inputLine.split(",")
        for(i <- 0 until values.length) {
          if(i < ArduinoActor.sensors.length && !values(i).isEmpty) {
            ArduinoActor.sensors(i).updateValue(Integer.parseInt(values(i)))
          }
        }

        if(measureIndex % 5 == 0) {
//          for(sensor <- ArduinoActor.sensors) {
//            print(sensor.delta + ", ")
//          }
//
//          println()

          var max = -1
          var maxIndex = -1

          for(i <- 0 until ArduinoActor.sensors.length) {
            val delta = ArduinoActor.sensors(i).delta()
            if(delta > 20 && delta > max) {
              max = delta
              maxIndex = i
            }
          }

          if(max > 0) {
            println("Sensor select " + maxIndex)
            var set = mutable.SortedSet[Button]()
            set += ArduinoActor.buttons(maxIndex)
            BoardActor() ! SensorSelect(set.toSet)
          } else {
              BoardActor() ! SensorSelect(mutable.SortedSet[Button]().toSet)

          }

        }
      }

      println("Exit loop")

      in.close()
    }
  }
}

class ArduinoActor extends Actor with ActorLogging {

//  val portId = "/dev/tty.usbmodem1411"
  val portId = "/dev/ttyACM0"
  var handler = new BinaryProtocol()

  println("Initializing Oracle Serial Protocol")

  def connect() = {
    try {
      println("Opening port at " + portId)
      val portIdentifier = CommPortIdentifier.getPortIdentifier(portId.toString)
      if (portIdentifier.isCurrentlyOwned) {
        println("Port is currently in use.")
      } else {
        val commPort = portIdentifier.open(this.getClass.getName, 2000)
        if (commPort.isInstanceOf[SerialPort]) {
          val serialPort = commPort.asInstanceOf[SerialPort]
          serialPort.setSerialPortParams(9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE)

          (new Thread(handler.getSerialReader(serialPort.getInputStream))).start
//          (new Thread(handler.getSerialWriter(serialPort.getOutputStream, serialPort.getInputStream))).start
        } else {
          println("Only serial ports are handled.")
        }
      }
    } catch {
      case e => println("Could not initialise serial port" + e.getMessage)
    }
  }
  override def preStart() = {
    try {
      connect()
    } catch {
      case error: UnsatisfiedLinkError => println("No RXTX lib loaded")
    }
  }

  override def postStop() = {

  }

  def receive = LoggingReceive {
    case other => log.info("unhandled: " + other)
  }
}

class SensorState {

  val calibrated = new Array[Int](1024)
  var calibratedIndex = 0
  val smooth = new Array[Int](10)
  var smoothIndex = 0

  def updateValue(value: Int): Unit = {
    if(smoothIndex < smooth.length - 1) {
      smoothIndex += 1
    } else {
      smoothIndex = 0

      //Average smooth add to calibrated
      if(calibratedIndex < calibrated.length - 1) {
        calibratedIndex += 1
      } else {
        calibratedIndex = 0
      }

      calibrated(calibratedIndex) = average(smooth)
    }

    smooth(smoothIndex) = value
  }

  def average(array : Array[Int]): Int = {
    var total = 0
    for (x <- array) {
      total += x
    }
    total / array.length
  }

  def delta() : Int = {
    average(smooth) - average(calibrated)
  }
}

object ArduinoActor {
  var sensors = new Array[SensorState](5)
  var buttons = Array(Button.Fire, Button.Aether, Button.Earth, Button.Air, Button.Water)
}