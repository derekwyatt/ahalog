package org.derekwyatt.ahalog

import akka.event.LoggingAdapter
import akka.http.scaladsl.model.DateTime
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives._
import akka.http.scaladsl.server.RouteResult.{ Complete, Rejected }
import akka.stream.Materializer
import akka.util.Timeout
import scala.concurrent.{ ExecutionContext, Future }

trait AhaLogDirectives extends BasicDirectives with MiscDirectives {
  def accessLog(log: LoggingAdapter)(implicit ec: ExecutionContext, to: Timeout, mat: Materializer): Directive0 =
    mapInnerRoute { originalRoute =>
      ctx => {
        def fromForwarded = ctx.request.header[`X-Forwarded-For`].flatMap(h => h.addresses.headOption)
        def fromRemoteAddress = ctx.request.header[`Remote-Address`].map(_.address)
        def fromRealIp = ctx.request.header[`X-Real-Ip`].map(_.address)

        def remoteAddress = fromForwarded orElse fromRemoteAddress orElse fromRealIp
        def remoteIp = remoteAddress.flatMap(ra => ra.toOption.map(_.getHostAddress)).getOrElse("-")

        def method = ctx.request.method.value
        def path = ctx.request.uri.toString
        def now = {
          val n = DateTime.now
          f"${n.day}%02d/${n.monthStr}/${n.year}:${n.hour}%02d:${n.minute}%02d:${n.second}%02d -0000"
        }
        def proto = ctx.request.protocol.value

        def mkString(code: String, size: String): String = s"""$remoteIp - - [$now] "$method $path $proto" $code $size"""

        originalRoute(ctx).flatMap {
          case rslt @ Complete(rsp) =>
            val code = rsp.status.intValue.toString
            rsp.entity.toStrict(to.duration).map(_.contentLength).map { size =>
              log.info(mkString(code, size.toString))
              rslt
            }
          case rslt @ Rejected(rejections) =>
            val reason = rejections.map(_.getClass.getName).mkString(",")
            mkString(reason, "-")
            Future.successful(rslt)
        }
      }
  }
}
