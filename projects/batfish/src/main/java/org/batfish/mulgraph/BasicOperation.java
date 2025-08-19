package org.batfish.mulgraph;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Stack;

public class BasicOperation {

    Digraph g;

    public BasicOperation(Digraph graph)
    {   
        g = graph;
    }

    // is dst reachable from src
    Boolean isReachable(Node src, Node dst) 
    { 
        Node temp;
        // Initially mark all vertices as not visited 
        //boolean visited[] = new boolean[nr_vertices];
        HashMap<Node, Boolean> visited = new HashMap<>(); 

        for (Node v : g.getVertices()) {
            visited.put(v, false);
        }
  
        // Create a stack for DFS 
        Stack<Node> stack = new Stack<>(); 
          
        // Push the current source node 
        stack.push(src); 
          
        while(stack.empty() == false) 
        { 
            // Pop a Node from stack and print it 
            temp = stack.peek(); 
            stack.pop(); 
            // Stack may contain same Node twice. So 
            // we need to print the popped item only 
            // if it is not visited. 
            if (visited.get(temp) == false) 
            { 
                visited.put(temp, true);
            }
              
            // Get all adjacent vertices of the popped Node s 
            // If a adjacent has not been visited, then puah it 
            // to the stack. 
            for(Edge e :  g.getNeighbors(temp)) {
                Node v = e.getDst();
                if (v == dst)
                    return true;
                if (visited.get(v) == false) 
                    stack.push(v); 
            } 
              
        }
        return false;
    } 

    // This function is a variation of DFSUytil() in  
    // https://www.geeksforgeeks.org/archives/18212 
    private boolean isCyclicUtil(Node i, HashMap<Node, Boolean> visited, 
                                      HashMap<Node, Boolean> recStack)  
    { 
          
        // Mark the current node as visited and 
        // part of recursion stack 
        if (recStack.get(i)) 
            return true; 
  
        if (visited.get(i)) 
            return false; 
              
        visited.put(i, true); 
  
        recStack.put(i, true); 
          
        for (Edge c: g.getNeighbors(i)) 
            if (isCyclicUtil(c.getDst(), visited, recStack)) 
                return true; 
                  
        recStack.put(i, false); 
  
        return false; 
    } 
  
    // Returns true if the graph contains a  
    // cycle, else false. 
    // This function is a variation of DFS() in  
    // https://www.geeksforgeeks.org/archives/18212 
    private boolean isOneCycle()  
    { 
          
        // Mark all the vertices as not visited and 
        // not part of recursion stack 
        HashMap<Node, Boolean> visited = new HashMap<>(); 
        HashMap<Node, Boolean> recStack = new HashMap<>(); 

        for (Node v : g.getVertices()) {
            visited.put(v, false);
        }
          
        for (Node v : g.getVertices()) {
            recStack.put(v, false);
        }
          
        // Call the recursive helper function to 
        // detect cycle in different DFS trees 
        for (Node v : g.getVertices()) {
            if (isCyclicUtil(v, visited, recStack)) 
                return true; 
        }
  
        return false; 
    } 

    // Prints all paths from 
    // 'src' to 'dst' 
    public ArrayList<Path> printAllPaths(Node src, Node dst)  
    { 
        // Initially mark all vertices as not visited 
        //boolean visited[] = new boolean[nr_vertices];
        HashMap<Node, Boolean> visited = new HashMap<>(); 

        for (Node v : g.getVertices()) {
            visited.put(v, false);
        }
        ArrayList<Node> pathList = new ArrayList<>(); 
        ArrayList<Edge> pathEdgeList = new ArrayList<>(); 
        ArrayList<Path> allPaths = new ArrayList<Path>();
        ArrayList<protocol> protocolList = new ArrayList<>();
        ArrayList<String> communitySeen = new ArrayList<>();
        //add source to path[] 
        pathList.add(src); 
          
        //Call recursive utility 
        printAllPathsUtil(src, dst, visited, pathList, pathEdgeList, allPaths, protocolList, communitySeen);

        return allPaths;

    } 
  
    // A recursive function to print 
    // all paths from 'u' to 'd'. 
    // visited keeps track of 
    // vertices in current path. 
    // localPathList<> stores actual 
    // vertices in the current path 
    private void printAllPathsUtil(Node u, Node d, 
        HashMap<Node, Boolean> visited, 
        ArrayList<Node> localPathList,
        ArrayList<Edge> pathEdgeList,
        ArrayList<Path> allPaths,
        ArrayList<protocol> protocolList,
        ArrayList<String> communitySeen) { 
          
        // Mark the current node 
        visited.put(u, true);

        // add communities by this Node
        communitySeen.addAll(u.addedCommunity);
        // remove communities by this Node
        communitySeen.removeAll(u.removedCommunity);

        //System.out.println("V " + u + " " + communitySeen);

        for (String comm : u.blockedCommunity) {
            if (communitySeen.contains(comm)) {
                for (String community : u.addedCommunity)
                    communitySeen.remove(community);
                //System.out.println("Blocked community at " + u);
                visited.put(u, false);
                return;
            }
        }
          
        if (u.equals(d))  
        { 
            //System.out.println(localPathList);
            /*
            ArrayList<Node> temp = (ArrayList<Node>) localPathList.clone();
            allPaths.add(temp);
            */
            ArrayList<Node> temp = new ArrayList<>();
            for ( Node t : localPathList) {
                temp.add(t);
            } 
            ArrayList<Edge> temp2 = new ArrayList<>();
            for ( Edge t : pathEdgeList) {
                temp2.add(t);
            } 
            Path pp = new Path(temp, temp2);
            allPaths.add(pp);
        } 
          
        // Recur for all the vertices 
        // adjacent to current Node 
        //System.out.println("V " + u + "  Edge: " + g.getNeighbors(u));
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
                localPathList.add(i);
                pathEdgeList.add(e);
                protocolList.add(e.getType());
                printAllPathsUtil(i, d, visited, localPathList, pathEdgeList, allPaths, protocolList, communitySeen); 
                  
                // remove current node 
                // in path[] 
                localPathList.remove(i);
                pathEdgeList.remove(e);
                protocolList.remove( protocolList.size() - 1);
            } 
        } 

        // remove added communities
        for (String comm : u.addedCommunity)
            communitySeen.remove(comm);
          
        // Mark the current node 
        visited.put(u, false);
    } 
}