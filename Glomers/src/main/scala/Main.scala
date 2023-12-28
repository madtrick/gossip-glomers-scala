package com.github.madtrick.glomers

import java.util.ArrayList
import scala.collection.mutable.ListBuffer
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.ConcurrentHashMap

class Broadcast {
  private var msgCounter             = 0
  private var nodeId: Option[NodeId] = None
  // TODO: check other list types (https://alvinalexander.com/scala/how-add-elements-to-a-list-in-scala-listbuffer-immutable/)
  private var messages: ListBuffer[Int]                                = new ListBuffer[Int]()
  private var neighbours: List[String]                                 = List()
  private val inflightMessages: ConcurrentHashMap[String, ujson.Value] = new ConcurrentHashMap()

  def log(msg: String): Unit = {
    System.err.println(msg)
  }
  def send(msg: String): Unit = {
    log(s"[send] $msg")
    println(msg)
  }

  def run(): Unit = {
    val executor = new ScheduledThreadPoolExecutor(1)
    val task = new Runnable {
      def run = {
        val keys = inflightMessages.keySet()
        keys.forEach((key: String) => {
          // System.err.println(s"[thread] $key")
          val message = inflightMessages.get(key)
          println(ujson.write(message))
        })
      }
    }

    executor.scheduleAtFixedRate(task, 0, 500, TimeUnit.MILLISECONDS)

    for (line <- io.Source.stdin.getLines()) {
      log(s"[recv] $line")
      val json = ujson.read(line)

      val src     = json("src").str
      val dest    = json("dest").str
      val msgType = json("body")("type").str
      val msgId   = json("body")("msg_id").num.toInt

      msgCounter += 1

      msgType match {
        case "init" => {
          nodeId = Some(NodeId(json("body")("node_id").str))
          // TODO: is there a more efficient way of converting the array of ujson.Value to a Seq[String]?
          val nodeIds = json("body")("node_ids").arr.map(_.str).toSeq
          neighbours = Broadcast.calculateNeighbours(nodeId.get, nodeIds, 3)
          send(
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
          send(
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
          val mapKey  = s"$src-$msgId"

          neighbours.foreach((neighbour) => {
            val deliveryMessage = ujson.Obj(
              "src"  -> nodeId.get.id,
              "dest" -> neighbour,
              "body" -> ujson.Obj(
                "type"    -> "deliver_broadcast",
                "message" -> message,
                "msg_id"  -> msgCounter,
                "origin"  -> nodeId.get.id
              )
            )

            msgCounter += 1

            inflightMessages.put(mapKey, deliveryMessage)
            send(ujson.write(deliveryMessage))
          })

          messages += message
          send(
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
        case "deliver_broadcast" => {
          val origin  = json("body")("origin").str
          val message = json("body")("message").num.toInt

          if (origin == nodeId.get.id) {
            log("[info] Skip message delivery as this node is origin")
          }

          neighbours
            // Filter the node that sent us the message. Avoid a loop
            .filter(_ != src)
            .foreach((neighbour) => {
              send(
                ujson.write(
                  ujson.Obj(
                    "src"  -> nodeId.get.id,
                    "dest" -> neighbour,
                    "body" -> ujson.Obj(
                      "type"    -> "deliver_broadcast",
                      "message" -> message,
                      "msg_id"  -> msgCounter,
                      "origin"  -> origin
                    )
                  )
                )
              )
            })

          messages += message

          send(
            ujson.write(
              ujson.Obj(
                "src"  -> dest,
                "dest" -> src,
                "body" -> ujson.Obj(
                  "type"        -> "deliver_broadcast_ok",
                  "in_reply_to" -> msgId,
                  "msg_id"      -> msgCounter
                )
              )
            )
          )
        }
        case "deliver_broadcast_ok" => {
          val mapKey = s"$src-$msgId"

          // Remove this message from the set of messages to be redelivered
          inflightMessages.remove(mapKey)
        }
        case "read" => {

          send(
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

object Broadcast {

  /** Calculates the set of neighbours for a given node. The neighbours are those nodes in the
    * cluster with whom the given node can talk with.
    *
    * A node can be the head of a neighbours group. This node can talk to all the nodes in the set
    * and to other neighbours group's head. Non head nodes can only talk to the head of their
    * neighbours group.
    *
    * Returns the ids of the neighbouring nodes.
    */
  def calculateNeighbours(
      nodeId: NodeId,
      nodes: Seq[String],
      neighbourSetSize: Int
  ): List[String] = {
    if (nodes.length == 1) {
      return List()
    }

    val candidates: ListBuffer[String] = ListBuffer()

    if (nodeId.nodeNumber % neighbourSetSize == 0) {
      var i = 1

      while (i < neighbourSetSize && (nodeId.nodeNumber + i < nodes.length)) {
        candidates += ("n" + (nodeId.nodeNumber + i))
        i += 1
      }

      var candidateNextHead = nodeId.nodeNumber + neighbourSetSize
      var candidatePrevHead = nodeId.nodeNumber - neighbourSetSize

      if (candidateNextHead >= nodes.length) {
        candidateNextHead = 0
      }

      if (candidatePrevHead < 0) {
        candidatePrevHead = Range(0, nodes.length).reverse
          .find((number) => {
            number % neighbourSetSize == 0
          })
          .get
      }

      if (candidatePrevHead == candidateNextHead && candidatePrevHead != nodeId.nodeNumber) {
        candidates += "n" + candidatePrevHead
      } else {
        if (candidatePrevHead != nodeId.nodeNumber) {
          candidates += "n" + candidatePrevHead
        }

        if (candidateNextHead != nodeId.nodeNumber) {
          candidates += "n" + candidateNextHead
        }
      }
    } else {
      // This is not a head node
      val offset = nodeId.nodeNumber % neighbourSetSize
      candidates += "n" + (nodeId.nodeNumber - offset)
    }

    return candidates.toList
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
