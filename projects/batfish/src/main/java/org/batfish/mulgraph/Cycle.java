package org.batfish.mulgraph;

import java.util.HashMap;
import java.util.ArrayList;

public class Cycle {

	Digraph g;

	public Cycle(Digraph graph)
	{	
		g = graph;
	}

    // check if cycle exists
    public Boolean isCyclic()  
    { 
        // Initially mark all vertices as not visited 
        //boolean visited[] = new boolean[nr_vertices];
        HashMap<Node, Boolean> visited = new HashMap<>(); 

        for (Node v : g.getVertices()) {
            visited.put(v, false);
        }
        ArrayList<protocol> protocolList = new ArrayList<>();  
          
        //Call recursive utility 
        for ( Node src : g.getVertices()) {
            if(isAnyCycleUtil(src, src, visited, protocolList)){
                return true;
            }
            visited.put(src, true);
        }

        return false;

    } 
  
    // A recursive function to detect if
    // there is a cycle from u

    private Boolean isAnyCycleUtil(Node u, Node d, 
        HashMap<Node, Boolean> visited, 
        ArrayList<protocol> protocolList) { 
    
        if (visited.get(d) == true) {      
            if (u.equals(d)) { 
                return true;
            }
            return false;
        }

        // Mark the current node 
        visited.put(d, true);
          
        // Recur for all the vertices 
        // adjacent to current vertex 
        for(Edge e: g.getNeighbors(d)) {

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
            // store current node  
            // in path[] 
            protocolList.add(e.getType());
            if (isAnyCycleUtil(u, i, visited, protocolList)) {
                return true;
            } 
              
            // remove current node 
            // in path[] 
            protocolList.remove( protocolList.size() - 1);

        } 
          
        // Mark the current node 
        visited.put(d, false);
        return false;
    } 

}