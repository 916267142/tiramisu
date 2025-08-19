package org.batfish.mulgraph;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
//import java.lang.*;

public class Tpg {

    /**
     * A Map is used to map each vertex to its list of adjacent vertices.
     */

    private Map<TpgNode, List<TpgEdge>> neighbors = new HashMap<>();

    private int nr_edges = 0;

    private int nr_vertices = 0;

    private TpgNode srcTCNode = null;

    private TpgNode dstTCNode = null;

    public Map<String, Set<TpgEdge>> logicalPhysicalMap = new HashMap<>();

    /**
     * String representation of graph.
     */
    public String toString() {
        StringBuffer s = new StringBuffer();
        for (TpgNode v : getVertices())
            s.append("\n    " + v + " -> " + neighbors.get(v));
        return s.toString();
    }

    public Map<TpgNode, List<TpgEdge>> getNeighborMap() {
      return neighbors;
    }

    public void setSourceDest(TpgNode srctc, TpgNode dsttc) {
      srcTCNode = srctc;
      dstTCNode = dsttc;
    }

    public TpgNode getSrc() {
      return srcTCNode;
    }

    public TpgNode getDst() {
      return dstTCNode;
    }

    public Map<TpgNode, List<TpgEdge>> returnCopyNeighborMap() {
      Map<TpgNode, List<TpgEdge>> copy = new HashMap<>();
      for (Map.Entry<TpgNode, List<TpgEdge>> entry: neighbors.entrySet()) {
        List<TpgEdge> copyList = new ArrayList<TpgEdge>();
        for (TpgEdge edge: entry.getValue()) {
          copyList.add(edge);
        }
        copy.put(entry.getKey(), copyList);
      }
      return copy;
    }

    public void setNeighborMap(Map<TpgNode, List<TpgEdge>> neighborMap) {
      neighbors = neighborMap;
    }

    /**
     * Add a Node to the graph. Nothing happens if Node is already in graph.
     */
    public void add(TpgNode vertex) {
        if (neighbors.containsKey(vertex))
            return;
        neighbors.put(vertex, new ArrayList<TpgEdge>());
        nr_vertices = nr_vertices + 1;

    }

    public int getNumberOfEdges(){
        int sum = 0;
        for(List<TpgEdge> outBounds : neighbors.values()){
            sum += outBounds.size();
        }
        return sum;
    }

    /**
     * True iff graph contains vertex.
     */
    public boolean contains(TpgNode vertex) {
        return neighbors.containsKey(vertex);
    }

    /**
     * Add an edge to the graph; if either vertex does not exist, it's added.
     * This implementation allows the creation of multi-edges and self-loops.
     */
    public TpgEdge add(TpgNode from, TpgNode to, EdgeCost cost, protocol type) {
        //System.out.println(from + "\t" + to);
        this.add(from);
        this.add(to);
        TpgEdge thisEdge = new TpgEdge(from, to, cost, type);
        neighbors.get(from).add(thisEdge);
        nr_edges = nr_edges + 1;
        return thisEdge;
    }

    public int outDegree(int vertex) {
        return neighbors.get(vertex).size();
    }

    public int inDegree(TpgNode vertex) {
       return inboundNeighbors(vertex).size();
    }

    public Set<TpgNode> getVertices() {
        return neighbors.keySet();
    }

    public TpgNode getVertex(String label) {
      for (TpgNode v : getVertices()) {
        if (v.getId().equals(label)) {
          return v;
        }
      }
      return null;
    }

    public List<TpgEdge> getNeighbors(TpgNode vertex) {
        return neighbors.get(vertex);
    }
    public List<TpgEdge> inboundEdges(TpgNode inboundVertex) {
        List<TpgEdge> inList = new ArrayList<>();
        for (TpgNode from : getVertices()) {
            for (TpgEdge e : neighbors.get(from))
                if (e.vertex.equals(inboundVertex))
                    inList.add(e);
        }
        return inList;
    }

    public List<TpgNode> outboundNeighbors(TpgNode vertex) {
        List<TpgNode> list = new ArrayList<TpgNode>();
        for(TpgEdge e: neighbors.get(vertex))
            list.add(e.vertex);
        return list;
    }

    public List<TpgNode> inboundNeighbors(TpgNode inboundNode) {
        List<TpgNode> inList = new ArrayList<TpgNode>();
        for (TpgNode from : getVertices()) {
            for (TpgEdge e : neighbors.get(from))
                if (e.vertex.equals(inboundNode))
                    inList.add(from);
        }
        return inList;
    }

    public boolean isEdge(TpgNode from, TpgNode to) {
      if (from == null || to ==null)
        return false;
      for(TpgEdge e :  neighbors.get(from)){
          if(e.vertex.equals(to))
              return true;
      }
      return false;
    }

    public TpgEdge getEdgeById(String from, String to) {
      for(TpgEdge e :  neighbors.get(getVertex(from))){
          if(e.vertex.getId().equals(to))
              return e;
      }
      return null;
    }

    public void setPhysicalMap(Map<String, Set<TpgEdge>> lmap) {
      logicalPhysicalMap = lmap;
    }

    public Map<String, Set<TpgEdge>> getPhysicalMap() {
      return logicalPhysicalMap;
    }

    public void printAllPhysicalMap() {
      System.out.println(logicalPhysicalMap);
    }

    public TpgEdge getEdge(TpgNode from, TpgNode to) {
      for(TpgEdge e :  neighbors.get(from)){
          if(e.vertex.equals(to))
              return e;
      }
      return null;
    }
}

