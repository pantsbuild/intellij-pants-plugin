package com.twitter.ideprobe.jsonrpc

import java.net.Socket
import java.util.concurrent.atomic.AtomicInteger
import com.twitter.ideprobe.Close
import com.twitter.ideprobe.jsonrpc.JsonRpc._
import com.twitter.ideprobe.jsonrpc.PayloadJsonFormat.SerializedJson
import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.Promise

final class JsonRpcConnection(channel: Channel) extends AutoCloseable {
  private val id = new AtomicInteger(0)
  private val requests = mutable.Map.empty[String, Promise[Response]]

  def sendRequest(method: String, params: SerializedJson): Future[Response] = {
    val id = this.id.incrementAndGet().toString
    val promise = Promise[Response]()
    requests += (id -> promise)
    send(Request(id, method, params))
    promise.future
  }

  def sendNotification(method: String, params: SerializedJson): Unit = {
    send(Request(id = null, method, params))
  }

  def sendError(request: Request, cause: Throwable): Unit = {
    if (!request.isNotification) {
      val data = JsonRpc.gson.toJson(cause.getStackTrace)
      val error = JsonRpc.Error(0, cause.getMessage, data)
      send(Response(request.id, error, result = PayloadJsonFormat.Null))
    }
  }

  def sendResponse(request: Request, result: SerializedJson): Unit = {
    if (request.isNotification) ()
    else send(Response(request.id, error = null, result))
  }

  private def send(message: Message): Unit = {
    channel.send(message)
  }

  def onRequest(f: Request => Unit): Unit = {
    channel.received.foreach {
      case request: Request =>
        f(request)
      case response: Response if requests.contains(response.id) =>
        requests(response.id).success(response)
        requests -= response.id
      case _: Response =>
      // TODO handle mismatched response (log/drop)
    }
  }

  def close(): Unit = {
    Close(channel)
  }
}

object JsonRpcConnection {
  def from(socket: Socket): JsonRpcConnection = {
    new JsonRpcConnection(new Channel(socket))
  }
}
