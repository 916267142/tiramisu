package org.batfish.mulgraph;

import java.io.IOException;
import java.util.ArrayList;

public class Test {

    public static void main(String[] args) throws IOException {

        Digraph graph = new Digraph();
        Unreachable unreachable = new Unreachable(graph);
        Cycle cycle = new Cycle(graph);
        //BasicOperation basic = new BasicOperation(graph);
        //KConnected<Integer> kconn = new KConnected<Integer>(graph);
        BF bf = new BF(graph);
        RYen yen = new RYen(graph);
        //ECMP ecmp = new ECMP(graph);

        //graph.add(0);
        Node v1 = new Node("v1", protocol.BGP);
        Node v2 = new Node("v2", protocol.BGP);
        Node v3 = new Node("v3", protocol.BGP);
        Node v4 = new Node("v4", protocol.BGP);
        Node v5 = new Node("v5", protocol.BGP);
        Node v6 = new Node("v6", protocol.BGP);
        Node v7 = new Node("v7", protocol.BGP);

        graph.add(v1);
        graph.add(v2);
        graph.add(v3);
        graph.add(v4);
        graph.add(v5);
        graph.add(v6);
        graph.add(v7);
        //graph.add(5);

        /*graph.add(0, 1, 1, protocol.IBGP);
        graph.add(v1, v2, 2, protocol.BGP);
        graph.add(v2, v3, 2, protocol.BGP);
        //graph.add(3, 0, 2);
        graph.add(v1, v4, 1, protocol.BGP);
        graph.add(v4, v5, 5, protocol.BGP);
        graph.add(v5, v3, 5, protocol.BGP);
        graph.addPhysicalMap(v1, v2, 1);
        graph.addPhysicalMap(v2, v3, 1);
        graph.addPhysicalMap(v4, v5, 1);
        */

        //*
        EdgeCost ec = new EdgeCost();
        ec.ospf_cost = 3;
        graph.add(v1, v2, ec, protocol.OSPF);
        graph.add(v2, v6, ec, protocol.OSPF);
        graph.add(v6, v7, ec, protocol.OSPF);
        graph.add(v2, v5, ec, protocol.OSPF);

        ec = new EdgeCost();
        ec.ospf_cost = 1;
        graph.add(v5, v7, ec, protocol.OSPF);

        ec = new EdgeCost();
        ec.ospf_cost = 6;
        graph.add(v1, v3, ec, protocol.OSPF);

        ec = new EdgeCost();
        ec.ospf_cost = 2;
        graph.add(v3, v7, ec, protocol.OSPF);

        ec = new EdgeCost();
        ec.ospf_cost = 10;
        graph.add(v1, v4, ec, protocol.OSPF);
        graph.add(v4, v7, ec, protocol.OSPF);

        //ec.ospf_cost = 10;

        //EdgeCost ec2 = new EdgeCost();
        //ec2.ospf_cost = 20;

        //graph.add(1, 2, ec, protocol.IBGP);

        //graph.setCorrelated();
        //yen.shortestPath(v1, v3);
        //*/
        /*
        EdgeCost ec = new EdgeCost();
        ec.as_length = 1;
        ec.lp = 10;
        graph.add(v1, v2, ec, protocol.BGP);
        graph.add(v2, v7, ec, protocol.BGP);
        graph.add(v1, v3, ec, protocol.BGP);
        graph.add(v3, v6, ec, protocol.BGP);
        graph.add(v6, v7, ec, protocol.BGP);

        ec = new EdgeCost();
        ec.as_length = 1;
        ec.lp = 5;
        graph.add(v1, v4, ec, protocol.BGP);
        graph.add(v4, v7, ec, protocol.BGP);


        ec = new EdgeCost();
        ec.as_length = 1;
        ec.lp = 15;
        graph.add(v1, v5, ec, protocol.BGP);
        graph.add(v5, v7, ec, protocol.BGP);
        */
        int k = 1;

        long startTime = System.nanoTime();
        ArrayList<Path> pp = yen.ksp(v1, v7, 4);
        long endTime = System.nanoTime();
        long duration = (endTime - startTime)/(1000000);
        System.out.println(duration + " ms");

        for (Path p : pp) {
            System.out.println(k + "." + p);
            k = k+1;
        }
        //System.out.println(bf.shortestPath(v1, v7));

        //yen.ksp(v1, v3, 2);
        //*/

        //kconn.formulate(1, 3);
        //kconn.run();

        /*
        EdgeCost ec = new EdgeCost();
        ec.as_length = 1;
        ec.lp = 10;
        graph.add(v1, v2, ec, protocol.BGP);
        graph.add(v2, v7, ec, protocol.BGP);
        graph.add(v1, v3, ec, protocol.BGP);
        graph.add(v3, v6, ec, protocol.BGP);
        graph.add(v6, v7, ec, protocol.BGP);

        ec = new EdgeCost();
        ec.as_length = 1;
        ec.lp = 5;
        graph.add(v1, v4, ec, protocol.BGP);
        graph.add(v4, v7, ec, protocol.BGP);


        ec = new EdgeCost();
        ec.as_length = 1;
        ec.lp = 15;
        graph.add(v1, v5, ec, protocol.BGP);
        graph.add(v5, v7, ec, protocol.BGP);

        System.out.println("Is cyclic? "+ cycle.isCyclic());
        System.out.println("Is unreachable? "+ unreachable.isUnreachable(v1, v3));
        System.out.println("Is reachable? "+ unreachable.isUnreachable(3, 0));
        System.out.println("Is reachable? "+ graph.isReachable(0, 3));
        System.out.println("Is reachable? "+ graph.isReachable(3, 0));
        System.out.println("The nr. of vertices is: " + graph.getVertices().size());
        System.out.println("The nr. of edges is: " + graph.getNumberOfEdges()); // to be fixed
        System.out.println("The current graph: " + graph);
        System.out.println("In-degrees for 0: " + graph.inDegree(0));
        System.out.println("Out-degrees for 0: " + graph.outDegree(0));
        System.out.println("In-degrees for 3: " + graph.inDegree(3));
        System.out.println("Out-degrees for 3: " + graph.outDegree(3));
        System.out.println("Outbounds for 1: "+ graph.outboundNeighbors(1));
        System.out.println("Inbounds for 1: "+ graph.inboundNeighbors(1));
        System.out.println("(0,2)? " + (graph.isEdge(0, 2) ? "It's an edge" : "It's not an edge"));
        System.out.println("(1,3)? " + (graph.isEdge(1, 3) ? "It's an edge" : "It's not an edge"));
    
        System.out.println("Cost for (1,3)? "+ graph.getCost(1, 3));
        System.out.println("Is reachable? "+ graph.isReachable(1, 3));
        System.out.println("Is reachable? "+ graph.isReachable(3, 1));
        System.out.println("Paths 1 to 3 ");
        ArrayList<ArrayList<Integer>> allPath = graph.printAllPaths(1, 3);

        for (ArrayList<Integer> localPathList: allPath) {
            System.out.println(localPathList);
        }
        System.out.println("Is cycle? "+ graph.isCyclic());
        */

    }
}
