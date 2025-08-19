package org.batfish.mulgraph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;

public class Unreachable {

	Digraph g;

	public Unreachable(Digraph graph)
	{	
		g = graph;
	}

    // check if src and dst are unreachable
    public Boolean isUnreachable(Node src, Node dst)  
    { 
        //g.removeEdge(g.getEdgeById("c-BGP", "b-BGP"));
        // Initially mark all vertices as not visited 
        //boolean visited[] = new boolean[nr_vertices];
        HashMap<Node, Boolean> visited = new HashMap<>(); 

        for (Node v : g.getVertices()) {
            visited.put(v, false);
        }
        //ArrayList<protocol> protocolList = new ArrayList<>();  
          
        //Call recursive utility 
        //return !(isReachableAnyPathUtil(src, dst, visited, protocolList)); 
        return !(isReachableAny(src, dst, visited)); 
    } 

    // A recursive function to detect if
    // 'u' is reachable to 'd'. 

    private Boolean isReachableAny(Node u, Node d, 
        HashMap<Node, Boolean> visited) { 
          
        // Mark the current Node 
        visited.put(u, true);
        //System.out.println(u + " - " +d);
        if (u.equals(d)) { 
            return true;
        } 

        // Recur for all the vertices 
        // adjacent to current Node 
        for(Edge e: g.getNeighbors(u)) {
            protocol current = e.getType(); 
            Node i = e.getDst();

            if (current == protocol.DEF) 
                continue;
            // reset the Vertexs to remove ospf-ospf pair and reset DEF to anything else
            //Set<Edge> removeEdges = new HashSet<>();
            if (current == protocol.IBGP) {
                for (Edge e1: g.getNeighbors(i)) {
                    protocol next = e1.getType(); 
                    if (next == protocol.OSPF) {
                        Node i2 = e1.getDst();
                        for (Edge e2: g.getNeighbors(i2)) {
                            protocol third = e2.getType();
                            Node i3 = e2.getDst();
                            if (third == protocol.DEF) {
                                //e2.type = protocol.BGP;
                                if (i.equals(d) || i2.equals(d)) {
                                    return true;
                                }
                                if (visited.get(i3) == false) {
                                    if (isReachableAny(i3, d, visited)) {
                                        return true;
                                    }                                         
                                }

                            } else if (third == protocol.BGP) {
                                if (i.equals(d) || i2.equals(d)) {
                                    return true;
                                }
                                if (visited.get(i3) == false) {
                                    if (isReachableAny(i3, d, visited)) {
                                        return true;
                                    }                                         
                                }
                            }
                        }
                    }
                }
                continue;
            }
            /*
            for (Edge remove : removeEdges) {
                System.out.println("Remove " + remove);
                g.removeEdgeGraph(remove);
            }*/
            if (visited.get(i) == false && (e.getType()!= protocol.DEF)) {
            // store current Node  
            // in path[] 
                if (isReachableAny(i, d, visited)) {
                    return true;
                } 
            } 
        } 

        return false;

          
    }
  
    // A recursive function to detect if
    // 'u' is reachable to 'd'. 

    private Boolean isReachableAnyPathUtil(Node u, Node d, 
        HashMap<Node, Boolean> visited, 
        ArrayList<protocol> protocolList) { 
          
        // Mark the current node 
        visited.put(u, true);
          
        if (u.equals(d))  
        { 
            return true;
        } 
          
        // Recur for all the vertices 
        // adjacent to current vertex 
        for(Edge e: g.getNeighbors(u)) {

            int currentSize = protocolList.size();
            protocol current = e.getType(); 

            if (currentSize >= 2) {
                protocol parent = protocolList.get(currentSize - 1);
                protocol grandparent = protocolList.get(currentSize - 2);
                // iBGP->OSPF->OSPF not allowed
                if (grandparent == protocol.IBGP) {
                    if (parent == protocol.OSPF && current == protocol.OSPF) { // do the same for static routes
                        continue;
                    }
                }
                // iBGP->OSPF->DEF is the only way DEF edge is traversed
                if (current == protocol.DEF) {
                    if (!(parent == protocol.OSPF && grandparent == protocol.IBGP)) { // do the same for static routes
                        continue;
                    }
                }

            } else if (current == protocol.DEF) {
                continue;
            }

            Node i = e.getDst();
            if (visited.get(i) == false) 
            { 
                // store current node  
                // in path[] 
                protocolList.add(e.getType());
                if (isReachableAnyPathUtil(i, d, visited, protocolList)) {
                    return true;
                } 
                  
                // remove current node 
                // in path[] 
                protocolList.remove( protocolList.size() - 1);
            } 
        } 
          
        // Mark the current node 
        visited.put(u, false);
        return false;
    } 

}
