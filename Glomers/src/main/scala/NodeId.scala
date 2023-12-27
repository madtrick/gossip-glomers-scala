package com.github.madtrick.glomers

case class NodeId(val id: String) {
  def nodeNumber: Int = {
    id.substring(1).toInt
  }
}
