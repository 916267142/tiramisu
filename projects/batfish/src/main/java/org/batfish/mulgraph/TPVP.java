package org.batfish.mulgraph;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;


public class TPVP implements Runnable {

	Tpg g;

    Map<TpgNode, EdgeCost> weight;
    Map<TpgNode, TpgNode> nextHop;
    Map<TpgNode, TpgPath> bestPath;
    Map<TpgNode, Map<TpgNode, EdgeCost>> nextPathCosts;
    HashSet<TpgEdge> failedSet;

    EdgeCost curBestWeight;

    TpgNode s, d;



	public TPVP(Tpg graph) {	
		g = graph;

        weight = new HashMap<>();
        nextHop = new HashMap<>();
        bestPath = new HashMap<>();
        failedSet = new HashSet<>();
        nextPathCosts = new HashMap<>();
        initializeGraph();
	}

    public TPVP(Tpg graph, TpgEdge fail, TpgNode src, TpgNode dst) {    
        g = graph;
        weight = new HashMap<>();
        nextHop = new HashMap<>();
        bestPath = new HashMap<>();
        failedSet = new HashSet<>();
        nextPathCosts = new HashMap<>();
        failedSet.add(fail);
        s = g.getVertex(src.getId());
        d = g.getVertex(dst.getId());        
    }


    public void initializeGraph() {

        // Step 1: Initialize distances from src to all other 
        // vertices as INFINITE 
        for(TpgNode v : g.getVertices()) {
            weight.put(v, new EdgeCost());
            weight.get(v).setInvalid();
            nextHop.put(v, null);
            nextPathCosts.put(v, new HashMap<>());
            bestPath.put(v, new TpgPath());
            for (TpgEdge e1 : g.getNeighbors(v)) {
                TpgNode next = e1.getDst(); 
                nextPathCosts.get(v).put(next, new EdgeCost());
                nextPathCosts.get(v).get(next).setInvalid();
            }
        }
    }

    public void addFailEdge(TpgEdge fail) {
        failedSet.add(fail);
    }

    public void removeFailEdge(TpgEdge fail) {
        failedSet.remove(fail);
    }

    public EdgeCost update(EdgeCost weight_u, EdgeCost dist, protocol type) {
        EdgeCost ec = new EdgeCost();
        if (type == protocol.INTF) {
            // copy edge value
            ec = weight_u.copy();
        } else if (type == protocol.OSPF) { 
            // copy all variable just update ospf
            ec = weight_u.copy();
            ec.setOSPF(weight_u.ospf_cost + dist.ospf_cost);
            ec.AD =  dist.AD;
        } else if (type == protocol.BGP) {
            // update MED, LP as edge value, update AS
            ec = dist.copy();
            ec.setAS(weight_u.as_length + dist.as_length);
            ec.lp = dist.lp;
            ec.lp = dist.med;
            ec.AD = dist.AD;
        } else if (type == protocol.SWITCH) {
            // copy edge value
            ec = weight_u.copy();
        } else if (type == protocol.STAT) {
            // nothing really
            ec = weight_u.copy();
            ec.AD = dist.AD;
        } else if (type == protocol.SRC) {
            // compare other vertices
            ec = weight_u.copy();
        } else if (type == protocol.DST) {
            // just use edge
            ec = weight_u.copy();
            //ec.AD = dist.AD;
            //ec.AD = Edge.protocol_map.get(protocol.DST);
        } else if (type == protocol.IBGP) {
            // update MED, LP as edge value, update AS
            ec = dist.copy();
            ec.setAS(weight_u.as_length);
            //ec.AD = dist.AD;
        } else if (type == protocol.RIB) {
            // update MED, LP as edge value, update AS
            ec = weight_u.copy();
            //ec.AD = dist.AD;
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
            if ( (x.AD == TpgEdge.protocol_map.get(protocol.BGP) && y.AD == TpgEdge.protocol_map.get(protocol.BGP)) ||
                (x.AD == TpgEdge.protocol_map.get(protocol.IBGP) && y.AD == TpgEdge.protocol_map.get(protocol.IBGP)) ){

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
            } else if (x.AD == TpgEdge.protocol_map.get(protocol.OSPF) && y.AD == TpgEdge.protocol_map.get(protocol.OSPF)) { // if it is ospf
                if (x.ospf_cost < y.ospf_cost) {
                    return true;
                } else if (x.ospf_cost > y.ospf_cost) {
                    return false;
                } else {
                    return false;
                }
            } else if ( (x.AD == TpgEdge.protocol_map.get(protocol.INTF) && y.AD == TpgEdge.protocol_map.get(protocol.INTF)) || 
                        (x.AD == TpgEdge.protocol_map.get(protocol.SWITCH) && y.AD == TpgEdge.protocol_map.get(protocol.SWITCH)) ) { // if it is ospf
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

    public TpgNode getBest(TpgNode curNode) {
        TpgNode best = null;
        for (TpgNode neighbor : nextPathCosts.get(curNode).keySet()) {
            EdgeCost cur = nextPathCosts.get(curNode).get(neighbor);
            if (cur.valid && compare(cur, curBestWeight)) {
                curBestWeight = cur;
                best = neighbor;
            }
        }
        return best;

    }

    // return shortest path
    public TpgPath shortestPath(TpgNode src, TpgNode dst) { 
        s = g.getVertex(src.getId());
        d = g.getVertex(dst.getId());
        //System.out.println(((Node)src).getId());
        //System.out.println(currWeight);

        EdgeCost currWeight;
        weight.put(d, new EdgeCost());
        weight.get(d).AD = TpgEdge.protocol_map.get(protocol.DST);
        weight.get(d).valid = true;
        nextHop.put(d, d);
        bestPath.get(d).add(d);

        //System.out.println("FailSet " + failedSet);

        // Step 2: Relax all edges |V| - 1 times. A simple 
        // shortest path from src to any other Node can 
        // have at-most |V| - 1 edges 
        //System.out.println("Starting weight calc");
        boolean changed = true;
        //for(Node vertices : g.getVertices()) {
        while (changed) {
          changed = false;

          for(TpgNode u : g.getVertices()) {

            EdgeCost weight_u = weight.get(u);
            for(TpgEdge e1 : g.getNeighbors(u)) {
                if (failedSet.contains(e1)) {
                    continue;
                }
                TpgNode v = e1.getDst(); 
                EdgeCost dist = e1.getCost();
                EdgeCost weight_v = weight.get(v);
                TpgPath path_v = bestPath.get(v);

                if ( (weight_v.valid) && !path_v.contains(u) ) {
                    currWeight = update(weight_v, dist, e1.getType());
                    nextPathCosts.get(u).put(v, currWeight);
                    
                    /*if (u.getId().equals("c-BGP")){
                        System.out.println(u + "\t" + currWeight+ "\t" + weight_u);
                        System.out.println(e1 + "  " + weight_v);
                    }*/
                } else {
                    nextPathCosts.get(u).get(v).setInvalid();
                }
            }

            curBestWeight = new EdgeCost();
            TpgNode best = getBest(u);
            if (best!= null) {
                if( (best!=nextHop.get(u)) && compare(curBestWeight, weight_u)) {
                    weight.put(u, curBestWeight);
                    nextHop.put(u, best);
                    TpgPath path_u = new TpgPath(bestPath.get(best));
                    path_u.add(u);
                    bestPath.put(u, path_u);
                    changed = true;
                    //System.out.println(u + "\t" + best + "\t" + curBestWeight);
                }
            }

          } //break;
        }

        if (weight.get(s).valid) {
            return bestPath.get(s);
        }
        return null;
        //*/
    } 

    public String createName(String router, String prefix) {
        return router+"-"+prefix;
    }

    public TpgPath getActualPath() {
        TpgPath actualPath = new TpgPath();
        Boolean done = false;
        TpgNode entryNode = s;
        while (!done) {
            String name = createName(entryNode.getDevice(), "RIB");
            TpgPath curPath = bestPath.get(g.getVertex(name));
            TpgNode curNode = curPath.getVertex(0);
            String curRouter = curNode.getDevice();
            for (TpgNode n : curPath.getTpgNodes()) {
                if (n.getId().equals(d.getId())) {
                    actualPath.addEnd(n);
                    done = true;
                    break;
                } else if (curRouter.equals(n.getDevice())) {
                    actualPath.addEnd(n);
                } else {
                    entryNode = n;
                    break;
                }
            }
        }
        return actualPath;
    }

    public HashSet<TpgEdge> getRemovalEdges(TpgPath curPath) {
        HashSet<TpgEdge> edgeSet = new HashSet<>();
        ArrayList<TpgNode> nodes = curPath.getTpgNodes();
        int size = nodes.size();
        for (int i = 0; i + 1 < size; i++) {
            TpgEdge edge = g.getEdge(nodes.get(i), nodes.get(i+1));
            if (edge != null && edge.canRemove) {
                edgeSet.add(edge);
            }
        }
        return edgeSet;
    }

    public HashSet<TpgEdge> getAllEdges() {
        Boolean done = false;
        TpgNode entryNode = s;
        HashSet<TpgEdge> edgeSet = new HashSet<>();
        while (!done && s!=null) {
            String name = createName(entryNode.getDevice(), "RIB");
            TpgPath curPath = bestPath.get(g.getVertex(name));
            TpgNode curNode = curPath.getVertex(0);
            String curRouter = curNode.getDevice();
            edgeSet.addAll(getRemovalEdges(curPath));
            for (TpgNode n : curPath.getTpgNodes()) {
                if (n.getId().equals(d.getId())) {
                    done = true;
                    break;
                } else if (!curRouter.equals(n.getDevice())) {
                    entryNode = n;
                    break;
                }
            }
        }
        return edgeSet;
    }



    // return shortest path
    public void run() { 
        initializeGraph();

        EdgeCost currWeight;
        weight.put(d, new EdgeCost());
        weight.get(d).AD = TpgEdge.protocol_map.get(protocol.DST);
        weight.get(d).valid = true;
        nextHop.put(d, d);
        bestPath.get(d).add(d);

        //System.out.println("FailSet " + failedSet);

        // Step 2: Relax all edges |V| - 1 times. A simple 
        // shortest path from src to any other Node can 
        // have at-most |V| - 1 edges 
        //System.out.println("Starting weight calc");
        boolean changed = true;
        //for(Node vertices : g.getVertices()) {
        while (changed) {
          changed = false;

          for(TpgNode u : g.getVertices()) {

            EdgeCost weight_u = weight.get(u);
            for(TpgEdge e1 : g.getNeighbors(u)) {
                if (failedSet.contains(e1)) {
                    continue;
                }
                TpgNode v = e1.getDst(); 
                EdgeCost dist = e1.getCost();
                EdgeCost weight_v = weight.get(v);
                TpgPath path_v = bestPath.get(v);

                if ( (weight_v.valid) && !path_v.contains(u) ) {
                    currWeight = update(weight_v, dist, e1.getType());
                    nextPathCosts.get(u).put(v, currWeight);
                    
                    /*if (u.getId().equals("c-BGP")){
                        System.out.println(u + "\t" + currWeight+ "\t" + weight_u);
                        System.out.println(e1 + "  " + weight_v);
                    }*/
                } else {
                    nextPathCosts.get(u).get(v).setInvalid();
                }
            }

            curBestWeight = new EdgeCost();
            TpgNode best = getBest(u);
            if (best!= null) {
                if( (best!=nextHop.get(u)) && compare(curBestWeight, weight_u)) {
                    weight.put(u, curBestWeight);
                    nextHop.put(u, best);
                    TpgPath path_u = new TpgPath(bestPath.get(best));
                    path_u.add(u);
                    bestPath.put(u, path_u);
                    changed = true;
                    //System.out.println(u + "\t" + best + "\t" + curBestWeight);
                }
            }

          } //break;
        }

    } 


}