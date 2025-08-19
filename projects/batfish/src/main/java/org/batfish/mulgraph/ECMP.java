package org.batfish.mulgraph;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;


public class ECMP {

	Digraph g;

	public ECMP(Digraph graph) {	
		g = graph;
	}

    public EdgeCost update(EdgeCost weight_u, EdgeCost dist) {
        EdgeCost ec = new EdgeCost();
        ec.setOSPF(weight_u.ospf_cost + dist.ospf_cost);
        return ec;
        //return weight_u.ospf_cost + dist.ospf_cost;
    }

    public int compare(EdgeCost x, EdgeCost y) {

        // lp1 > lp2: static > ospf > BGP ; as1 < as2
        if (x.AD < y.AD) {
            return 1;
        } else if (x.AD > y.AD) {
            return -1;
        } else {
            // if it is BGP
            if (x.as_length != Integer.MAX_VALUE) {

                if (x.lp > y.lp) {
                    return 1;
                } else if (x.lp < y.lp) {
                    return -1;
                } else {
                    if (x.as_length < y.as_length) {
                        return 1;
                    } else if (x.as_length > y.as_length) {
                        return -1;
                    } else {
                        //System.out.println("Same BGP");
                        return 0;
                    }
                }
            } else if (x.ospf_cost != Integer.MAX_VALUE) { // if it is ospf

                if (x.ospf_cost < y.ospf_cost) {
                    return 1;
                } else if (x.ospf_cost > y.ospf_cost) {
                    return -1;
                } else {
                    //System.out.println("Same OSPF");
                    return 0;
                }
            }
        }
        //System.out.println("Same WEIGHT");
        return 0;
    }

    public void printAll(Node src, Node cur, ArrayList<Node> pathNode, ArrayList<Edge> pathEdge, Map<Node, Boolean> visited, Map<Node, List<Node>> nextHop, ArrayList<Path> allPath) {
        if(visited.get(cur)) {
            return;
        }
        pathNode.add(cur);
        visited.put(cur, true);
        if (cur.getId().equals(src.getId())) {
            Path temp = new Path(pathNode, pathEdge);
            allPath.add(temp);
        }
        for ( Node v : nextHop.get(cur)) {
            pathEdge.add(g.getEdge(v, cur));
            printAll(src, v, pathNode, pathEdge, visited, nextHop, allPath);
            pathEdge.remove(pathEdge.size()-1);
        }
        pathNode.remove(pathNode.size()-1);
        visited.put(cur, false);
    }


    // return shortest path
    public ArrayList<Path> shortestPath(Node src, Node dst) { 

        Map<Node, EdgeCost> weight = new HashMap<Node, EdgeCost>();
        Map<Node, List<Node>> nextHop = new HashMap<Node, List<Node>>();
        Map<Node, Boolean> visited = new HashMap<Node, Boolean>();
        EdgeCost currWeight = new EdgeCost();
        Path path = new Path();
        //System.out.println(((Node)src).getId());
        //System.out.println(currWeight);
        //*

        // Step 1: Initialize distances from src to all other 
        // vertices as INFINITE 
        for(Node v : g.getVertices()) {
            weight.put(v, new EdgeCost());
            nextHop.put(v, new ArrayList<Node>());
            visited.put(v, false);

        }
        EdgeCost ec = new EdgeCost();
        ec.setOSPF(0);
        weight.put(src, ec);
        nextHop.get(src).add(src);

        // Step 2: Relax all edges |V| - 1 times. A simple 
        // shortest path from src to any other vertex can 
        // have at-most |V| - 1 edges 
        for(Node vertices : g.getVertices()) { 
          for(Node u : g.getVertices()) {
            for(Edge e1 : g.getNeighbors(u)) {

                Node v = e1.getDst(); 
                EdgeCost dist = e1.getCost();
                EdgeCost weight_u = weight.get(u);
                EdgeCost weight_v = weight.get(v);
                if ( (weight_u.valid == true) ) {
                    currWeight = update(weight_u, dist);
                    // add hasECMP
                    if (compare(currWeight, weight_v) == 0) {
                        weight.put(v, currWeight); 
                        if (!nextHop.get(v).contains(u)) {
                            nextHop.get(v).add(u);
                        }
                    } else if (compare(currWeight, weight_v) == 1) {
                        weight.put(v, currWeight); 
                        ArrayList<Node> temp = new ArrayList<Node>();
                        temp.add(u);
                        nextHop.put(v, temp);
                    }
                }
            }
          } 
        }

        ArrayList<Node> curPathNode = new ArrayList<Node>(); 
        ArrayList<Edge> curPathEdge = new ArrayList<Edge>(); 
        ArrayList<Path> allPath = new ArrayList<Path>(); 
        printAll(src, dst, curPathNode, curPathEdge, visited, nextHop, allPath);

        /*
        System.out.println("Paths");
        for (Path p: allPath) {
            System.out.println(p);
        }
        */

        return allPath;
    } 

}