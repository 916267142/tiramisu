package org.batfish.mulgraph;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;


public class BF {

	Digraph g;

    Map<Node, EdgeCost> weight;
    Map<Node, Node> nextHop;
    Map<Node, Set<Node>> prevHop;
    Map<Node, Set<String>> couldSee;
    Map<Node, Map<String, Boolean>> seen;

	public BF(Digraph graph) {	
		g = graph;

        weight = new HashMap<Node, EdgeCost>();
        nextHop = new HashMap<Node, Node>();
        prevHop = new HashMap<>();
        couldSee = g.getCouldSeeCommunity();
        seen = new HashMap<>();

        initializeGraph();
	}

    public Map<Node, Map<String, Boolean>> getSeen() {
        return seen;
    }

    public void setSeen(Node v, Map<String, Boolean> seenComm) {
        seen.put(v, seenComm);
    }

    public void initializeGraph() {

        // Step 1: Initialize distances from src to all other 
        // vertices as INFINITE 
        for(Node v : g.getVertices()) {
            weight.put(v, new EdgeCost());
            nextHop.put(v, null);
            prevHop.put(v, new HashSet<>());
        }

        for (Map.Entry<Node, Set<String>> entry : couldSee.entrySet()) {
            Node key = entry.getKey();
            Map<String, Boolean> seenCommunity = new HashMap<>();
            seen.put(key, seenCommunity);
            for (String comm : entry.getValue()) {
                if (key.addedCommunity.contains(comm))
                    seen.get(key).put(comm, true);
                else
                    seen.get(key).put(comm, false);
            }
        }

    }

    public EdgeCost update(EdgeCost weight_u, EdgeCost dist, protocol type) {
        EdgeCost ec = new EdgeCost();
        if (type == protocol.OSPF) { 
            // copy all variable just update ospf
            ec = weight_u.copy();
            ec.setOSPF(weight_u.ospf_cost + dist.ospf_cost);
            ec.AD =  dist.AD;
            ec.valid = true;

        } else if (type == protocol.BGP) {
            // update MED, LP as edge value, update AS
            ec = dist.copy();
            ec.setAS(weight_u.as_length + dist.as_length);
            ec.AD = dist.AD;
            ec.valid = true;
        } else if (type == protocol.SWITCH) {
            // copy edge value
            ec = weight_u.copy();
            ec.valid = true;
            ec.AD = dist.AD;
        } else if (type == protocol.STAT) {
            // nothing really
            ec.AD = dist.AD;
            ec.valid = true;
        } else if (type == protocol.IBGP) {
            //same as BGP but don't update AS
            ec = weight_u.copy();
            ec.AD = dist.AD;
            ec.valid = true;
        } else if (type == protocol.DEF) {
            //just copy values
            ec = weight_u.copy();
            ec.AD = dist.AD;
            ec.valid = true;
        } else if (type == protocol.REDISOB ||
                   type == protocol.REDISBO ||
                   type == protocol.REDISSB ||
                   type == protocol.REDISSO ) {
            // just use edge
            ec = dist.copy();
            ec.AD = dist.AD;
            ec.valid = true;
        } else if (type == protocol.SRC) {
            // compare other vertices
            ec = weight_u.copy();
            ec.valid = true;
        } else if (type == protocol.DST) {
            // just use edge
            ec = dist.copy();
            //ec.AD = dist.AD;
            ec.AD = Edge.protocol_map.get(protocol.DST);
            ec.valid = true;
        }
        return ec;
    }

    public Boolean compare(EdgeCost x, EdgeCost y) {

        // lp1 > lp2: static > ospf > BGP ; as1 < as2
        if (x.AD < y.AD) {
            return true;
        } else if (x.AD > y.AD) {
            return false;
        } else {
            // if it is BGP
            if ( (x.AD == Edge.protocol_map.get(protocol.BGP) && y.AD == Edge.protocol_map.get(protocol.BGP)) ||
                (x.AD == Edge.protocol_map.get(protocol.IBGP) && y.AD == Edge.protocol_map.get(protocol.IBGP)) ){

                if (x.lp > y.lp) {
                    return true;
                } else if (x.lp < y.lp) {
                    return false;
                } else {
                    if (x.as_length < y.as_length) {
                        return true;
                    } else if (x.as_length > y.as_length) {
                        return false;
                    } else {
                        if (x.med < y.med) {
                            return true;
                        } else if (x.med > y.med) {
                            return false;
                        }
                        return false;
                    }
                }
            } else if (x.AD == Edge.protocol_map.get(protocol.OSPF) && y.AD == Edge.protocol_map.get(protocol.OSPF)) { // if it is ospf
                if (x.ospf_cost < y.ospf_cost) {
                    return true;
                } else if (x.ospf_cost > y.ospf_cost) {
                    return false;
                } else {
                    return false;
                }
            } else if (x.AD == Edge.protocol_map.get(protocol.SWITCH) && y.AD == Edge.protocol_map.get(protocol.SWITCH)) { // if it is ospf
                if (x.lp > y.lp) {
                    return true;
                } else if (x.lp < y.lp) {
                    return false;
                } else {
                    if (x.as_length < y.as_length) {
                        return true;
                    } else if (x.as_length > y.as_length) {
                        return false;
                    } else {
                        if (x.med < y.med) {
                            return true;
                        } else if (x.med > y.med) {
                            return false;
                        } else {
                            if (x.ospf_cost < y.ospf_cost) {
                                return true;
                            } else if (x.ospf_cost > y.ospf_cost) {
                                return false;
                            } else {
                                return false;
                            }
                        }
                    }
                }

            }
        }
        //System.out.println("Same WEIGHT");
        return false;
    }

    public void addToPrevHop(Map<Node, Set<Node>> prevHop, Node newv, Node u, Node oldv) {
        if (oldv != null) {
            prevHop.get(oldv).remove(u);
        }
        prevHop.get(newv).add(u);
    }

    public void removeAllNext(Map<Node, Set<Node>> prevHop,
     Map<Node, Node> nextHop, Node v, String comm, Map<Node, Map<String, Boolean>> seen, Map<Node, EdgeCost> edgeWeight) {

        for (Iterator<Node> iterator = prevHop.get(v).iterator(); iterator.hasNext();) {
            Node next = iterator.next();
            nextHop.put( next, null);
            prevHop.get(v).remove(next);
            edgeWeight.put( next, new EdgeCost());
            removeAllNext(prevHop, nextHop, next, comm, seen, edgeWeight);
        }
    }


    public void resetSeen(Map<Node, Set<Node>> prevHop,
     Map<Node, Node> nextHop, Node u, String comm, Map<Node, Map<String, Boolean>> seen, Map<Node, EdgeCost> edgeWeight) {

        for (Iterator<Node> iterator = prevHop.get(u).iterator(); iterator.hasNext();) {
            Node prev = iterator.next();

            if (prev.blockedCommunity.contains(comm)) {
                nextHop.put( prev, null);
                prevHop.get(u).remove(prev);
                edgeWeight.put( prev, new EdgeCost());
                removeAllNext(prevHop, nextHop, prev, comm, seen, edgeWeight);
            } else if (!prev.addedCommunity.contains(comm)) {
                seen.get(prev).put(comm, true);
                resetSeen(prevHop, nextHop, prev, comm, seen, edgeWeight);
            }

        }        

    }

    // return shortest path
    public Path shortestPath(Node src, Node dst) { 
        //g.removeEdge(g.getEdgeById("c-BGP", "b-BGP"));
        g.setCorrelated();
        EdgeCost currWeight = new EdgeCost();
        Path path = new Path();
        Node s = g.getVertex(src.getId());
        Node d = g.getVertex(dst.getId());
        //System.out.println(((Node)src).getId());
        //System.out.println(currWeight);
        /*

        EdgeCost ec = new EdgeCost();
        if (src.getType() == protocol.OSPF) {
            ec.setOSPF(0);
            ec.setAD(20);
        }
        else if (src.getType() == protocol.BGP) {
            ec.setAS(0);
            ec.setAD(30);
        }*/
        //System.out.println(g);
        EdgeCost ec = new EdgeCost();
        ec.valid = true;
        weight.put(d, ec);
        nextHop.put(d, d);

        // Step 2: Relax all edges |V| - 1 times. A simple 
        // shortest path from src to any other Node can 
        // have at-most |V| - 1 edges 
        //System.out.println("Starting weight calc");
        boolean changed = true;
        //for(Node vertices : g.getVertices()) {
        while (true) {
          if (changed == false) {
            break;
          }
          changed = false;
          for(Node u : g.getVertices()) {
            for(Edge e1 : g.getNeighbors(u)) {

                Node v = e1.getDst(); 
                EdgeCost dist = e1.getCost();
                EdgeCost weight_u = weight.get(u);
                EdgeCost weight_v = weight.get(v);

                // FIX IBGP-DEF
                if (e1.getType() == protocol.IBGP) {
                    boolean canAllow = false;
                    if (nextHop.get(v) != null && nextHop.get(v) != v ) {
                        protocol edgeType = g.getEdge(v, nextHop.get(v)).getType();
                        if (edgeType != protocol.OSPF) {
                            canAllow = true;
                            //System.out.println("ibgp " + e1);    
                        }
                        if (edgeType == protocol.OSPF) {
                            Node next = nextHop.get(v);
                            if (nextHop.get(next) != null && nextHop.get(next) != next ) {
                                protocol edgeType2 = g.getEdge(next, nextHop.get(next)).getType();
                                if (edgeType2 == protocol.DEF) {
                                    canAllow = true;
                                }
                            }
                        }
                    }
                    if (canAllow == false)
                        continue;
                }

                if (e1.getType() == protocol.DEF && !g.defUsed.contains(e1)) {
                    continue;
                }

                if (e1.getType() == protocol.OSPF) {
                    Node nextnext = nextHop.get(v);
                    if ( nextnext!=null && !nextnext.equals(v)) {
                        if (g.secondOSPF.contains(g.getEdge(v, nextnext))) {
                            continue;
                        }
                    }
                }

                boolean blockCommunity = false;
                if (couldSee.containsKey(v)) {
                    for ( String comm : couldSee.get(v) ) {
                        if (seen.get(v).get(comm) && u.blockedCommunity.contains(comm)) {
                            blockCommunity = true;
                            break;
                        }
                    }
                }

                if (blockCommunity)
                    continue;


                if ( (weight_v.valid == true) ) {
                    currWeight = update(weight_v, dist, e1.getType());
                    
                    /*if (u.getId().equals("c-BGP")){
                        System.out.println(u + "\t" + currWeight+ "\t" + weight_u);
                        System.out.println(e1 + "  " + weight_v);
                    }*/

                    if (compare(currWeight, weight_u)) {
                        //System.out.println(u + "\t" + v + "\t" + currWeight);
                        weight.put(u, currWeight);
                        addToPrevHop(prevHop, v, u, nextHop.get(u));
                        nextHop.put(u, v);
                        changed = true;
                        if (couldSee.containsKey(v)) {
                            for ( String comm : couldSee.get(v) ) {
                                if ( !( u.removedCommunity.contains(comm) || u.addedCommunity.contains(comm)) ) {
                                    seen.get(u).put(comm, seen.get(v).get(comm) ); 
                                    // remove advertisement which use u for next hop and block on comm
                                    if (seen.get(u).get(comm)) {
                                        resetSeen(prevHop, nextHop, u, comm, seen, weight);
                                    }
                                }
                            }
                        }



                    }
                }
            }
          } //break;
        }

        //System.out.println("Path finding");
        Node cur = s;
        path.add(s);
        //System.out.println(s+ "\t" + d);
        //System.out.println(nextHop);
        //System.out.println("Path");
        //System.out.print(cur);
        //System.out.print("--");
        HashMap<Node, Boolean> visitedInPath = new HashMap<>(); 

        for (Node v : g.getVertices()) {
            visitedInPath.put(v, false);
        }


        while( cur!= d ) {
            if (nextHop.get(cur) == null) {
                break;
            }
            if (visitedInPath.get(cur) == true) {
                System.out.println("cycle");
                break;
            }            
            path.add(nextHop.get(cur), g.getEdge(cur, nextHop.get(cur)));
            visitedInPath.put(cur, true);
            cur = nextHop.get(cur);

            //System.out.print(cur);
            //System.out.print("--");
        }
        //path.setPath();
        //System.out.println(path);
        if (cur.getId().equals(d.getId())) {
            //path.setPath();
            return path;
        }
        //System.out.println("Path doesn't exist" + s + " " + d);
        return null;
        //*/
    } 

}