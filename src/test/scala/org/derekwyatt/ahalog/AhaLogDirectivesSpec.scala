package org.derekwyatt.ahalog

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.RemoteAddress
import akka.http.scaladsl.server.{ Directives, Route }
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.{ ActorMaterializer, Materializer }
import akka.util.Timeout
import java.net.InetAddress
import org.scalatest.{ FlatSpec, Matchers }
import scala.concurrent.duration._

class TestLogger extends LoggingAdapter {

  def getInstance = this

  var lastLog: String = ""

  override def isErrorEnabled = true
  override def isWarningEnabled = true
  override def isInfoEnabled = true
  override def isDebugEnabled = true

  protected override def notifyError(message: String): Unit = lastLog = message
  protected override def notifyError(cause: Throwable, message: String): Unit = lastLog = message
  protected override def notifyWarning(message: String): Unit = lastLog = message
  protected override def notifyInfo(message: String): Unit = lastLog = message
  protected override def notifyDebug(message: String): Unit = lastLog = message
}

class AhaLogDirectivesSpec extends FlatSpec with Matchers with AhaLogDirectives with Directives with ScalatestRouteTest {
  val mat = ActorMaterializer()
  val to = Timeout(10.seconds)

  val logger = new TestLogger
  val myaddr = InetAddress.getLocalHost().getHostAddress()
  val remoteAddr = RemoteAddress(InetAddress.getLocalHost())

  def route: Route = accessLog(logger)(system.dispatcher, to, mat) {
    complete("the length of this is 24")
  }

  "AhaLogDirectives" should "log properly with a X-Real-Ip" in {
    Get() ~> `X-Real-Ip`(remoteAddr) ~> route ~> check {
      logger.lastLog should fullyMatch regex (myaddr + """ - - \[\d{2}/\w{3}/\d{4}:\d{2}:\d{2}:\d{2} -0000\] "GET http://example.com/ HTTP/1.1" 200 24""")
    }
  }

  it should "log properly with an X-Forwarded-For" in {
    Get() ~> `X-Forwarded-For`(remoteAddr) ~> route ~> check {
      logger.lastLog should fullyMatch regex (myaddr + """ - - \[\d{2}/\w{3}/\d{4}:\d{2}:\d{2}:\d{2} -0000\] "GET http://example.com/ HTTP/1.1" 200 24""")
    }
  }

  it should "log properly with a Remote-Address" in {
    Get() ~> `Remote-Address`(remoteAddr) ~> route ~> check {
      logger.lastLog should fullyMatch regex (myaddr + """ - - \[\d{2}/\w{3}/\d{4}:\d{2}:\d{2}:\d{2} -0000\] "GET http://example.com/ HTTP/1.1" 200 24""")
    }
  }

  it should "log properly no address at all" in {
    Get() ~> route ~> check {
      logger.lastLog should fullyMatch regex """- - - \[\d{2}/\w{3}/\d{4}:\d{2}:\d{2}:\d{2} -0000\] "GET http://example.com/ HTTP/1.1" 200 24"""
    }
  }
}
