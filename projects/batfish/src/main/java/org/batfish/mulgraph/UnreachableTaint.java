package org.batfish.mulgraph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;

public class UnreachableTaint {

	Digraph g;

	public UnreachableTaint(Digraph graph)
	{	
		g = graph;
	}

    public void taint(Node n, Map<Node, List<Edge>> taintNeighbor, HashMap<Node, Boolean> visited) {
        n.taint = true;
        visited.put(n, true);
        for (Edge e : taintNeighbor.get(n)) {
            if (e.getType() == protocol.IBGP || e.getType() == protocol.DEF || e.getType() == protocol.NONE) {
                continue;
            }

            Node next = e.getDst();
            if (visited.get(next) == false) {
                taint(next, taintNeighbor, visited);
            }
        }
        if (g._ibgpPeers.containsKey(n)) {
            for (Node next : g._ibgpPeers.get(n)) {
                if (visited.get(next) == false) {
                    taint(next, taintNeighbor, visited);
                }                
            }
        }
    }

    // check if src and dst are unreachable
    public Boolean isUnreachable(Node src, Node dst)  
    { 
        // Initially mark all vertices as not visited 
        //boolean visited[] = new boolean[nr_vertices];
        HashMap<Node, Boolean> visited = new HashMap<>(); 
        //Map<Node, List<Edge>> neighbor = g.getNeighborMap();
        //Map<Node, List<Edge>> neighbor = g.returnCopyNeighborMap();
        Map<Node, List<Edge>> reverseNeighbor = g.getReverseNeighbors();
        //taint(dst, reverseNeighbor, visited);

        for (Node v : g.getVertices()) {
            visited.put(v, false);
        }

        //dst.taint = true;
        taint(dst, reverseNeighbor, visited);
        System.out.println();
        visited = new HashMap<>(); 
        for (Node v : g.getVertices()) {
            visited.put(v, false);
        }
        int num_taint = 0;
        //ArrayList<protocol> protocolList = new ArrayList<>();  
          
        //Call recursive utility 
        //return !(isReachableAnyPathUtil(src, dst, visited, protocolList)); 
        return !(isReachableAny(src, dst, visited, num_taint)); 
    } 

    // A recursive function to detect if
    // 'u' is reachable to 'd'. 

    private Boolean isReachableAny(Node u, Node d, 
        HashMap<Node, Boolean> visited, int num_taint) { 
          
        // Mark the current Node 
        visited.put(u, true);
        //System.out.println(u + " - " +d);
        if (u.equals(d)) { 
            return true;
        } 

        if (u.taint == false) {
            num_taint = num_taint + 1;
        } else {
            num_taint = 0;
        }

        if (num_taint > 2) {
            //System.out.println("More than 3 taints");
            return false;
        }

        // Recur for all the vertices 
        // adjacent to current Node 
        for(Edge e: g.getNeighbors(u)) {
            protocol current = e.getType(); 
            Node i = e.getDst();

            if (visited.get(i) == false) {
            // store current Node  
            // in path[] 
                if (isReachableAny(i, d, visited, num_taint)) {
                    return true;
                } 
            } 
        } 

        return false;
    }
  

}
