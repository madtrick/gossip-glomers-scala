package com.github.madtrick.glomers

import java.util.ArrayList
import scala.collection.mutable.ListBuffer

class Broadcast {
  private var msgCounter             = 0
  private var nodeId: Option[String] = None
  // TODO: check other list types (https://alvinalexander.com/scala/how-add-elements-to-a-list-in-scala-listbuffer-immutable/)
  private var messages: ListBuffer[Int] = new ListBuffer[Int]()

  def run(): Unit = {
    for (line <- io.Source.stdin.getLines()) {
      val json = ujson.read(line)

      val src     = json("src").str
      val dest    = json("dest").str
      val msgType = json("body")("type").str
      val msgId   = json("body")("msg_id").num.toInt

      msgCounter += 1

      msgType match {
        case "init" => {
          nodeId = Some(json("body")("node_id").str)
          println(
            ujson.write(
              ujson.Obj(
                "src"  -> dest,
                "dest" -> src,
                "body" -> ujson.Obj(
                  "type"        -> "init_ok",
                  "in_reply_to" -> msgId,
                  "msg_id"      -> msgCounter
                )
              )
            )
          )
        }
        case "topology" => {
          println(
            ujson.write(
              ujson.Obj(
                "src"  -> dest,
                "dest" -> src,
                "body" -> ujson.Obj(
                  "type"        -> "topology_ok",
                  "in_reply_to" -> msgId,
                  "msg_id"      -> msgCounter
                )
              )
            )
          )

        }
        case "broadcast" => {
          val message = json("body")("message").num.toInt

          messages += message
          println(
            ujson.write(
              ujson.Obj(
                "src"  -> dest,
                "dest" -> src,
                "body" -> ujson.Obj(
                  "type"        -> "broadcast_ok",
                  "in_reply_to" -> msgId,
                  "msg_id"      -> msgCounter
                )
              )
            )
          )
        }
        case "read" => {

          println(
            ujson.write(
              ujson.Obj(
                "src"  -> dest,
                "dest" -> src,
                "body" -> ujson.Obj(
                  "type"        -> "read_ok",
                  "messages"    -> messages,
                  "in_reply_to" -> msgId,
                  "msg_id"      -> msgCounter
                )
              )
            )
          )
        }
      }
    }
  }
}

object Main extends App {
  if (sys.env.get("GLOMER").get == "broadcast") {
    new Broadcast().run()
  } else {
    var msgCounter             = 0
    var nodeId: Option[String] = None

    for (line <- io.Source.stdin.getLines()) {
      val json = ujson.read(line)

      val src     = json("src").str
      val dest    = json("dest").str
      val msgType = json("body")("type").str
      val msgId   = json("body")("msg_id").num.toInt

      msgCounter += 1

      msgType match {
        case "init" => {
          nodeId = Some(json("body")("node_id").str)
          println(
            ujson.write(
              ujson.Obj(
                "src"  -> dest,
                "dest" -> src,
                "body" -> ujson.Obj(
                  "type"        -> "init_ok",
                  "in_reply_to" -> msgId,
                  "msg_id"      -> msgCounter
                )
              )
            )
          )
        }
        case "echo" => {
          val echo = json("body")("echo").str

          println(
            ujson.write(
              ujson.Obj(
                "src"  -> dest,
                "dest" -> src,
                "body" -> ujson.Obj(
                  "type"        -> "echo_ok",
                  "msg_id"      -> msgCounter,
                  "in_reply_to" -> msgId,
                  "echo"        -> echo
                )
              )
            )
          )
        }
        case "generate" => {
          println(
            ujson.write(
              ujson.Obj(
                "src"  -> dest,
                "dest" -> src,
                "body" -> ujson.Obj(
                  "type"        -> "generate_ok",
                  "msg_id"      -> msgCounter,
                  "in_reply_to" -> msgId,
                  "id"          -> (nodeId.get + "_" + msgCounter)
                )
              )
            )
          )
        }
      }

    }

  }
}
