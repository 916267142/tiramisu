package org.batfish.mulgraph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;

public class UnreachableTpg {

	Tpg g;

	public UnreachableTpg(Tpg graph)
	{	
		g = graph;
	}

    // check if src and dst are unreachable
    public Boolean isUnreachableTpg(TpgNode src, TpgNode dst)  
    { 
        // Initially mark all vertices as not visited 
        HashMap<TpgNode, Boolean> visited = new HashMap<>(); 

        for (TpgNode v : g.getVertices()) {
            visited.put(v, false);
        }
        return !(isReachableAny(src, dst, visited)); 
    } 

    // A recursive function to detect if
    // 'u' is reachable to 'd'. 

    private Boolean isReachableAny(TpgNode u, TpgNode d, 
        HashMap<TpgNode, Boolean> visited) { 
          
        // Mark the current TpgNode 
        visited.put(u, true);
        //System.out.println(u + " - " +d);
        if (u.equals(d)) { 
            return true;
        } 

        // Recur for all the vertices 
        // adjacent to current TpgNode 
        for(TpgEdge e: g.getNeighbors(u)) {
            protocol current = e.getType(); 
            TpgNode i = e.getDst();

            if (visited.get(i) == false) {
                if (isReachableAny(i, d, visited)) {
                    return true;
                } 
            } 
        } 
        return false;          
    }
}
