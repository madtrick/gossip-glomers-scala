import org.scalatest.funsuite.AnyFunSuite
import com.github.madtrick.glomers.Broadcast
import com.github.madtrick.glomers.NodeId

class SetSuite extends AnyFunSuite {
  test("no neighbours") {
    val nodes = List("n0")

    assert(Broadcast.calculateNeighbours(NodeId("n0"), nodes, 3) == List())

  }

  test("no children") {
    val nodes = List("n0", "n1", "n2")

    assert(Broadcast.calculateNeighbours(NodeId("n2"), nodes, 2) == List("n0"))
  }

  test("cluster head") {
    val nodes = List("n0", "n1", "n2", "n3")

    assert(Broadcast.calculateNeighbours(NodeId("n0"), nodes, 3) == List("n1", "n2", "n3"))
  }

  test("non cluster head") {
    val nodes = List("n0", "n1", "n2", "n3")

    assert(
      Broadcast.calculateNeighbours(NodeId("n1"), nodes, 3)
        == List("n0")
    )
  }

  test("three clusters with children (cluster size 2)") {
    val nodes = List("n0", "n1", "n2", "n3", "n4")

    assert(Broadcast.calculateNeighbours(NodeId("n0"), nodes, 2) == List("n1", "n4", "n2"))
    assert(Broadcast.calculateNeighbours(NodeId("n1"), nodes, 2) == List("n0"))
    assert(Broadcast.calculateNeighbours(NodeId("n2"), nodes, 2) == List("n3", "n0", "n4"))
    assert(Broadcast.calculateNeighbours(NodeId("n3"), nodes, 2) == List("n2"))
    assert(Broadcast.calculateNeighbours(NodeId("n4"), nodes, 2) == List("n2", "n0"))
  }

  test("two clusters with children (cluster size 3)") {
    val nodes = List("n0", "n1", "n2", "n3", "n4")

    assert(Broadcast.calculateNeighbours(NodeId("n0"), nodes, 3) == List("n1", "n2", "n3"))
    assert(Broadcast.calculateNeighbours(NodeId("n1"), nodes, 3) == List("n0"))
    assert(Broadcast.calculateNeighbours(NodeId("n2"), nodes, 3) == List("n0"))
    assert(Broadcast.calculateNeighbours(NodeId("n3"), nodes, 3) == List("n4", "n0"))
    assert(Broadcast.calculateNeighbours(NodeId("n4"), nodes, 3) == List("n3"))
  }

  test("one node per cluster") {

    val nodes = List("n0", "n1", "n2")

    assert(Broadcast.calculateNeighbours(NodeId("n0"), nodes, 1) == List("n2", "n1"))
    assert(Broadcast.calculateNeighbours(NodeId("n1"), nodes, 1) == List("n0", "n2"))
    assert(Broadcast.calculateNeighbours(NodeId("n2"), nodes, 1) == List("n1", "n0"))
  }
}
