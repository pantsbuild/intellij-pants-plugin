package com.twitter.ideprobe.jsonrpc

import java.io.Reader
import com.google.gson._
import com.twitter.ideprobe.jsonrpc.PayloadJsonFormat._
import pureconfig.ConfigConvert
import scala.reflect.ClassTag

object JsonRpc {
  private val Version = "2.0"
  private[jsonrpc] val gson = new Gson

  def append(message: Message, output: Appendable): Unit = {
    gson.toJson(message, output)
  }

  def stream(input: Reader): Iterator[Message] = new Iterator[Message] {
    val jsonStream = new JsonStreamParser(input)

    override def hasNext: Boolean = {
      try jsonStream.hasNext
      catch {
        case _: JsonIOException =>
          false
      }
    }

    override def next(): Message = {
      jsonStream.next() match {
        case json: JsonObject if json.get("method") == null =>
          gson.fromJson(json, classOf[Response])
        case json: JsonObject =>
          gson.fromJson(json, classOf[Request])
        case _: JsonArray =>
          ??? // TODO handle batch requests/responses
        case _ =>
          ??? // TODO handle unexpected token
      }
    }
  }

  object Notification {
    def unapply(request: Request): Option[Request] = {
      if (request.isNotification) Some(request)
      else None
    }
  }

  object Failure {
    def unapply(response: Response): Option[Error] = {
      Option(response.error)
    }
  }

  sealed trait Message
  final case class Error(code: Int, message: String, data: SerializedJson = PayloadJsonFormat.Null)
  final case class Response(id: String, error: Error, result: SerializedJson, jsonrpc: String = Version) extends Message
  final case class Request(id: String, method: String, params: SerializedJson, jsonrpc: String = Version)
      extends Message {
    def isNotification: Boolean = id == null

    override def toString: String = {
      if (isNotification) s"Notification($method,$params,$jsonrpc)"
      else s"Request($id,$method,$params,$jsonrpc)"
    }
  }

  sealed abstract class Method[Parameters: ClassTag: ConfigConvert, Result: ClassTag: ConfigConvert] {
    def name: String

    def apply(f: Parameters => Result): SerializedJson => SerializedJson = { json =>
      val decoded = PayloadJsonFormat.fromJson[Parameters](json)
      val result = f(decoded)
      PayloadJsonFormat.toJson[Result](result)
    }

    def unapply(request: Request): Option[Parameters] = {
      if (request.method != name) None
      else PayloadJsonFormat.fromJsonOpt[Parameters](request.params)
    }

    def encode(parameters: Parameters): SerializedJson = {
      PayloadJsonFormat.toJson[Parameters](parameters)
    }

    def decode(json: SerializedJson): Result = {
      PayloadJsonFormat.fromJson[Result](json)
    }
  }

  object Method {
    case class Notification[Parameters: ClassTag: ConfigConvert](name: String) extends Method[Parameters, Unit]
    case class Request[Parameters: ClassTag: ConfigConvert, Result: ClassTag: ConfigConvert](name: String)
        extends Method[Parameters, Result]
  }

  final class Handler(dispatch: Map[String, SerializedJson => SerializedJson] = Map.empty) {
    def on[A, B](method: Method[A, B])(f: A => B): Handler = {
      new Handler(dispatch + (method.name -> method.apply(f)))
    }

    def apply(method: String, json: SerializedJson): SerializedJson = {
      if (dispatch.contains(method)) dispatch(method)(json)
      else throw new Exception(s"Method $method not implemented")
    }
  }
}
