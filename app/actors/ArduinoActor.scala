package actors

import java.io._
import java.util.concurrent.TimeUnit

import akka.actor.{Props, ActorLogging, Actor, ActorRef}
import akka.event.LoggingReceive
import gnu.io.{SerialPort, CommPortIdentifier}
import model.{SensorSelect, Button}
import model.Button._
import org.influxdb.InfluxDBFactory
import org.influxdb.dto.Point
import play.api.Play

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

  val statsUrl = Play.current.configuration.getString("influx.url").getOrElse("http://192.168.56.101:8086")

  val influxDB = InfluxDBFactory.connect(statsUrl, "root", "root")
  val minDelta = 15

  val dbName = "oracleSensors"
  influxDB.createDatabase(dbName)

  // Flush every 2000 Points, at least every 100ms
  influxDB.enableBatch(2000, 100, TimeUnit.MILLISECONDS);

  for(i <- 0 until ArduinoActor.sensors.length) {
    ArduinoActor.sensors(i) = new SensorState()
  }

  class SerialReader(in: InputStream) extends Runnable {
    def run: Unit = {

      val reader = new BufferedReader( new InputStreamReader(in))
      var inputLine = ""
      var measureIndex = 0
      val emptyMessage = SensorSelect(mutable.SortedSet[Button]().toSet)

      while ((inputLine = reader.readLine()) != null ) {
        measureIndex += 1

        val values = inputLine.split(",")
        for(i <- 0 until values.length) {
          if(i < ArduinoActor.sensors.length && !values(i).isEmpty) {
            ArduinoActor.sensors(i).updateValue(Integer.parseInt(values(i)))
          }
        }

        // Update for every third measurement
        if(measureIndex % 3 == 0) {
          val point1 : Point = Point.measurement("sensors")
            .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
            .addField("fire", ArduinoActor.sensors(0).delta())
            .addField("aether", ArduinoActor.sensors(1).delta())
            .addField("earth", ArduinoActor.sensors(2).delta())
            .addField("air", ArduinoActor.sensors(3).delta())
            .addField("water", ArduinoActor.sensors(4).delta())
            .build()

          influxDB.write("oracleSensors", "autogen", point1)

          var max = -1
          var maxIndex = -1

          for(i <- 0 until ArduinoActor.sensors.length) {
            val delta = ArduinoActor.sensors(i).delta()

            if(delta > minDelta && delta > max) {
              max = delta
              maxIndex = i
            }
          }

          var lastMessage : SensorSelect = null

          if(max > 0) {
            var set = mutable.SortedSet[Button]()
            set += ArduinoActor.buttons(maxIndex)

            val newMessage = SensorSelect(set.toSet)

            if(!newMessage.equals(lastMessage)) {
              BoardActor() ! newMessage
            }
          } else if(lastMessage != emptyMessage) {
              lastMessage = emptyMessage
              BoardActor() ! emptyMessage
          }
        }
      }

      in.close()
    }
  }
}

class ArduinoActor extends Actor with ActorLogging {

  val portId = Play.current.configuration.getString("tty.port").getOrElse("/dev/ttyACM0")
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

  def receive = LoggingReceive {
    case other => log.info("unhandled: " + other)
  }
}

class SensorState {

  val calibrated = new Array[Int](512)
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
    var count = array.length + 1
    for (x <- array) {
      total += x
      if(x == 0) {
        count -= 1
      }
    }
    total / count
  }

  def delta() : Int = {
    average(smooth) - average(calibrated)
  }
}

object ArduinoActor {
  var sensors = new Array[SensorState](5)
  var buttons = Array(Button.Fire, Button.Aether, Button.Earth, Button.Air, Button.Water)
}