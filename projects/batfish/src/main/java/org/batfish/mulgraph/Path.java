package org.batfish.mulgraph;

import java.util.ArrayList;
import java.util.Collections;

public class Path {

	ArrayList<Node> pathNodes;
	ArrayList<Edge> pathEdges;

	public Path() {	
		pathNodes = new ArrayList<Node>();
		pathEdges = new ArrayList<Edge>();
	}

	public Path(ArrayList<Node> nodes, ArrayList<Edge> edges) {	
        pathNodes = new ArrayList<Node>();
        pathEdges = new ArrayList<Edge>();
        for (Node v : nodes)
            pathNodes.add(v);

        for (Edge e : edges)
            pathEdges.add(e);
	}

	public void add(Node n) {	
		pathNodes.add(n);
	}

	public void add(Node n, Edge e) {	
		pathNodes.add(n);
		pathEdges.add(e);	
	}

	public void setPath() {	
		Collections.reverse(pathNodes);
		Collections.reverse(pathEdges);
	}

	public Node getVertex(int i) {
		if (i > pathNodes.size())
			return null;
		return pathNodes.get(i);
	}

	public Edge getEdge(int i) {
		if (i > pathEdges.size())
			return null;
		return pathEdges.get(i);
	}

	public ArrayList<Node> getNodes() {
		return pathNodes;
	}

    public int getEdgesize() {
        return pathEdges.size();
    }

    public ArrayList<Edge> getEdges() {
        return pathEdges;
    }

    @Override
    public String toString() {
        return "Path Nodes:" + pathNodes + "\tPath Edges:" + pathEdges;
        //return "Path Nodes:" + pathNodes;
    }

    public Path copy() {
		ArrayList<Node> nodes = new ArrayList<Node>();
		ArrayList<Edge> edges = new ArrayList<Edge>();
        for (Edge edge : pathEdges) {
            edges.add(edge.copy());
        }

        for (Node vertex : pathNodes) {
        	Node n = vertex.copy();
            nodes.add(n);
        }
        return new Path(nodes, edges);
	}

    @Override
    public Path clone() {
        ArrayList<Node> nodes = new ArrayList<Node>();
        ArrayList<Edge> edges = new ArrayList<Edge>();

        for (Edge e : pathEdges) {
            edges.add(e.copy());
        }

        for (Node v : pathNodes) {
            nodes.add(v.copy());
        }

        return new Path(nodes, edges);
    }

    public Path cloneTo(int i) {
		ArrayList<Node> nodes = new ArrayList<Node>();
		ArrayList<Edge> edges = new ArrayList<Edge>();

        int l = pathEdges.size();
        if (i > l)
            i = l;

        //for (Edge edge : this.edges.subList(0,i)) {
        for (int j = 0; j < i; j++) {
            edges.add(pathEdges.get(j).copy());
        }

        for (int j = 0; j < i+1; j++) {
            nodes.add(pathNodes.get(j).copy());
        }

        return new Path(nodes, edges);
    }

    public Path cloneFrom(int i) {

		ArrayList<Node> nodes = new ArrayList<Node>();
		ArrayList<Edge> edges = new ArrayList<Edge>();

        int l = pathEdges.size();
        if (i > l)
            i = l;

        //for (Edge edge : this.edges.subList(0,i)) {
		int size = pathEdges.size();
        for (int j = (size - i); j < size; j++) {
            edges.add(pathEdges.get(j).copy());
        }

        for (int j = (size -i) ; j < size+1; j++) {
            nodes.add(pathNodes.get(j).copy());
        }

        return new Path(nodes, edges);
	}

    public boolean equals(Path path2) {
        if (path2 == null)
            return false;

        ArrayList<Node> nodes = path2.getNodes();

        int numNodes1 = pathNodes.size();
        int numNodes2 = nodes.size();

        if (numNodes1 != numNodes2) {
            return false;
        }

        for (int i = 0; i < numNodes1; i++) {
            Node node1 = pathNodes.get(i);
            Node node2 = nodes.get(i);
            if (!node1.getId().equals(node2.getId()))
                return false;
        }

        return true;
    }

    public void addPath(Path p2) {
        // ADD CHECK TO SEE THAT PATH P2'S FIRST NODE IS SAME AS THIS PATH'S LAST NODE
        if(!pathNodes.get(pathNodes.size()-1).getId().equals(p2.getNodes().get(0).getId())) {
            System.out.println("Nodes don't match. Can't append"+ pathNodes + p2.getNodes());
            return;
        }
        //System.out.println(p2.getNodes() + "" + p2.getNodes().subList(1,3) );
        pathNodes.addAll(p2.getNodes().subList(1, p2.getNodes().size()));
        pathEdges.addAll(p2.getEdges());
    }        

}