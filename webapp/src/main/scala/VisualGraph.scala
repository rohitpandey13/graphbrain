package com.graphbrain.webapp

import com.graphbrain.hgdb.UserOps
import com.graphbrain.hgdb.Vertex
import com.graphbrain.hgdb.Edge
import com.graphbrain.hgdb.TextNode
import com.graphbrain.hgdb.URLNode
import com.graphbrain.hgdb.UserNode
import com.graphbrain.hgdb.ID

import com.graphbrain.hgdb.VertexStore
import com.graphbrain.hgdb.UserManagement
import com.graphbrain.hgdb.SimpleCaching
import scala.math


object VisualGraph {

  def generate(rootId: String, store: UserOps, user: UserNode) = {
    val userId = if (user != null) user.id else ""
    
    // get neighboring edges
    val hyperEdges = store.neighborEdges2(rootId, userId)
    
    // map hyperedges to visual edges
    val visualEdges = hyperEdges.map(hyper2edge(_, rootId)).filter(_ != null)

    // group nodes by edge type
    val edgeNodeMap = generateEdgeNodeMap(visualEdges, rootId)

    // create map with all information for supernodes
    val snodeMap = generateSnodeMap(edgeNodeMap, store)

    // create reply structure with all the information needed for rendering
    val reply = Map(("root" -> rootId), ("snodes" -> snodeMap))

    Server.store.clear()

    // generate json reply
    JSONGen.json(reply)
  }

  private def generateEdgeNodeMap(edges: Set[Edge], rootId: String) = {
    edges.map(
      e => e.participantIds
        .zip(0 until e.participantIds.length)
        .map(x => (e.edgeType, x._2, x._1))
    ).flatten
      .filter(x => x._3 != rootId)
      .groupBy(x => (x._1, x._2))
      .mapValues(x => x.map(y => y._3))
  }

  private def hyper2edge(edge: Edge, rootId: String) = {
    if (edge.participantIds.length > 2) {
      if (edge.edgeType == "rtype/1/instance_of~owned_by") {
        if (edge.participantIds(0) == rootId) {
          Edge("rtype/1/has", List(edge.participantIds(2), rootId))
        }
        else if (edge.participantIds(2) == rootId) {
          Edge("rtype/1/has", List(rootId, edge.participantIds(0)))
        }
        else {
          null
        }
      }
      else {
        val edgeType = edge.edgeType.replaceAll("~", " .. ") + " .. "
        Edge(edgeType, List(edge.participantIds(0), edge.participantIds(1)))
      }
    }
    else {
      edge
    }
  }

  private def node2map(nodeId: String, store: UserOps) = {
    val node = try {
      store.get(nodeId)
    }
    catch {
      case _ => null
    }
    node match {
      case tn: TextNode => Map(("type" -> "text"), ("text" -> tn.text))
      case un: URLNode => {
        val title = if (un.title == "") un.url else un.title
        Map(("type" -> "url"), ("text" -> title), ("url" -> un.url), ("icon" -> un.icon))
      }
      case un: UserNode => Map(("type" -> "user"), ("text" -> un.name))
      case null => ""
      case _ => Map(("type" -> "text"), ("text" -> node.id))
    }
  }

  private def generateSnode(pair: ((String, Int), Set[String]), store: UserOps) = {
    val id = pair._1._1 + " " + pair._1._2
    val label = linkLabel(pair._1._1)
    val color = linkColor(label)
    val nodes = pair._2.map(node2map(_, store))

    val data = Map(("nodes" -> nodes), ("label" -> label), ("color" -> color))

    id -> data
  }

  private def generateSnodeMap(edgeNodeMap: Map[(String, Int), Set[String]], store: UserOps) = {
    edgeNodeMap.map(x => generateSnode(x, store))
  }

  private def linkColor(label: String) = {
    val index = math.abs(label.hashCode) % Colors.colors.length
    Colors.colors(index)
  }

  private def linkLabel(edgeType: String): String = {
    if (edgeType == "")
      return ""
    val lastPart = ID.parts(edgeType).last
    lastPart.replace("_", " ")
  }

  def main(args: Array[String]) {
    val store = new VertexStore with SimpleCaching with UserOps with UserManagement
      
    println(VisualGraph.generate("1/eraserhead", store, null))

    sys.exit(0)
  }
}