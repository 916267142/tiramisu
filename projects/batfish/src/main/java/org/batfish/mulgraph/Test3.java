package org.batfish.mulgraph;

import java.io.IOException;

public class Test3 {

    public static void main(String[] args) throws IOException {

        Digraph graph = new Digraph();
        //Unreachable unreachable = new Unreachable(graph);
        //Cycle cycle = new Cycle(graph);
        //BasicOperation basic = new BasicOperation(graph);
        //Disconnect kconn = new Disconnect(graph);
        //BellmanFord bf = new BellmanFord(graph);
        

        Node v1 = new Node("v1", protocol.BGP);
        Node v2 = new Node("v2", protocol.BGP);
        Node v3 = new Node("v3", protocol.BGP);
        Node v4 = new Node("v4", protocol.BGP);
        Node v5 = new Node("v5", protocol.BGP);
        Node v6 = new Node("v6", protocol.BGP);
        Node v7 = new Node("v7", protocol.BGP);
        Node v8 = new Node("v8", protocol.BGP);
        Node v9 = new Node("v9", protocol.BGP);
        Node v10 = new Node("v10", protocol.BGP);
        Node v11 = new Node("v11", protocol.BGP);
        Node v12 = new Node("v12", protocol.BGP);

        /*
        v1.addCommunity(10);
        v3.addCommunity(20);
        v10.blockCommunity(10);
        v10.blockCommunity(20);
        */
        //*
        v8.addCommunity("10");
        //v2.addCommunity(10);
        v9.removeCommunity("10");
        v10.blockCommunity("10");
        //*/
        graph.add(v1);
        graph.add(v2);
        graph.add(v3);
        graph.add(v4);
        graph.add(v5);
        graph.add(v6);
        graph.add(v7);
        graph.add(v8);
        graph.add(v9);
        graph.add(v10);
        //graph.add(v11);
        //graph.add(v12);

        //*
        EdgeCost ec = new EdgeCost();
        ec.setAS(3);

        graph.add(v1, v2, ec, protocol.BGP);
        graph.add(v2, v3, ec, protocol.BGP);
        graph.add(v3, v4, ec, protocol.BGP);
        //graph.add(v1, v5, ec, protocol.BGP);
        //graph.add(v5, v3, ec, protocol.BGP);
        //graph.add(v1, v6, ec, protocol.BGP);
        //graph.add(v6, v10, ec, protocol.BGP);
        //graph.add(v7, v4, ec, protocol.BGP);*/
        graph.add(v1, v8, ec, protocol.BGP);
        graph.add(v8, v9, ec, protocol.BGP);
        graph.add(v9, v10, ec, protocol.BGP);
        graph.add(v10, v4, ec, protocol.BGP);
        
        //graph.setCommunity();
        //Verification vf = new Verification(graph);
        RPathLength kconn = new RPathLength(graph);
        //RKConnected kconn = new RKConnected(graph);

        //System.out.println("Always blocked? " + vf.alwaysBlocked(v1, v6));
        kconn.formulate(v1, v4, 1);
        //kconn.formulate(v1, v4);
        kconn.run();


    }
}
