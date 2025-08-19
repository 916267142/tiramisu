package org.batfish.mulgraph;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
//import java.lang.*;

public class Rag {

    /**
     * A Map is used to map each vertex to its list of adjacent vertices.
     */

    private Map<RagNode, List<RagEdge>> neighbors = new HashMap<>();

    private int nr_edges = 0;

    private int nr_vertices = 0;

    private RagNode srcTCNode = null;

    private RagNode dstTCNode = null;

    public Map<RagNode, Set<RagNode>> _ibgpPeers = new HashMap<>();
    /**
     * String representation of graph.
     */
    public String toString() {
        StringBuffer s = new StringBuffer();
        for (RagNode v : getVertices())
            s.append("\n    " + v + " -> " + neighbors.get(v));
        return s.toString();
    }

    public Map<RagNode, List<RagEdge>> getNeighborMap() {
      return neighbors;
    }

    public void setSourceDest(RagNode srctc, RagNode dsttc) {
      srcTCNode = srctc;
      dstTCNode = dsttc;
    }

    public RagNode getSrc() {
      return srcTCNode;
    }

    public RagNode getDst() {
      return dstTCNode;
    }

    public void setNeighborMap(Map<RagNode, List<RagEdge>> neighborMap) {
      neighbors = neighborMap;
    }

    /**
     * Add a Node to the graph. Nothing happens if Node is already in graph.
     */
    public void add(RagNode vertex) {
        if (neighbors.containsKey(vertex))
            return;
        neighbors.put(vertex, new ArrayList<RagEdge>());
        nr_vertices = nr_vertices + 1;

    }

    public int getNumberOfEdges(){
        int sum = 0;
        for(List<RagEdge> outBounds : neighbors.values()){
            sum += outBounds.size();
        }
        return sum;
    }

    /**
     * True iff graph contains vertex.
     */
    public boolean contains(RagNode vertex) {
        return neighbors.containsKey(vertex);
    }

    /**
     * Add an edge to the graph; if either vertex does not exist, it's added.
     * This implementation allows the creation of multi-edges and self-loops.
     */
    public void add(RagNode from, RagNode to, protocol type) {
        //System.out.println(from + "\t" + to);
        this.add(from);
        this.add(to);
        neighbors.get(from).add(new RagEdge(from, to, type));
        nr_edges = nr_edges + 1;
    }

    /**
     * Add both side edges to the graph
     */
    public void diAdd(RagNode from, RagNode to, protocol type) {
        this.add(from);
        this.add(to);
        neighbors.get(from).add(new RagEdge(from, to, type));
        neighbors.get(to).add(new RagEdge(to, from, type));
        nr_edges = nr_edges + 2;
    }

    public int outDegree(int vertex) {
        return neighbors.get(vertex).size();
    }

    public int inDegree(RagNode vertex) {
       return inboundNeighbors(vertex).size();
    }

    public Set<RagNode> getVertices() {
        return neighbors.keySet();
    }

    public RagNode getVertex(String label) {
      for (RagNode v : getVertices()) {
        if (v.getId().equals(label)) {
          return v;
        }
      }
      return null;
    }

    public List<RagEdge> getNeighbors(RagNode vertex) {
        return neighbors.get(vertex);
    }
    public List<RagEdge> inboundEdges(RagNode inboundVertex) {
        List<RagEdge> inList = new ArrayList<>();
        for (RagNode from : getVertices()) {
            for (RagEdge e : neighbors.get(from))
                if (e.vertex.equals(inboundVertex))
                    inList.add(e);
        }
        return inList;
    }

    public List<RagNode> outboundNeighbors(RagNode vertex) {
        List<RagNode> list = new ArrayList<RagNode>();
        for(RagEdge e: neighbors.get(vertex))
            list.add(e.vertex);
        return list;
    }

    public List<RagNode> inboundNeighbors(RagNode inboundNode) {
        List<RagNode> inList = new ArrayList<RagNode>();
        for (RagNode from : getVertices()) {
            for (RagEdge e : neighbors.get(from))
                if (e.vertex.equals(inboundNode))
                    inList.add(from);
        }
        return inList;
    }

    public boolean isEdge(RagNode from, RagNode to) {
      if (from == null || to ==null)
        return false;
      for(RagEdge e :  neighbors.get(from)){
          if(e.vertex.equals(to))
              return true;
      }
      return false;
    }

    public RagEdge getEdgeById(String from, String to) {
      for(RagEdge e :  neighbors.get(getVertex(from))){
          if(e.vertex.getId().equals(to))
              return e;
      }
      return null;
    }

    public RagEdge getEdge(RagNode from, RagNode to) {
      for(RagEdge e :  neighbors.get(from)){
          if(e.vertex.equals(to))
              return e;
      }
      return null;
    }

    public void taint() {
      taint_propagate(dstTCNode, false);
    }

    public void taint_propagate(RagNode r, boolean ibgp) {
      r.taint = true;
      for (RagEdge e :  neighbors.get(r)) {
        if (e.getType() == protocol.IBGP) {
          if (ibgp == false && !e.getDst().taint) {
            taint_propagate(e.getDst(), true);
          }
        } else {
          if (!e.getDst().taint) {
            taint_propagate(e.getDst(), false);
          }
        }
      }
    }

    public boolean isTainted(RagNode v) {
      return v.getTaint();
    }


}

