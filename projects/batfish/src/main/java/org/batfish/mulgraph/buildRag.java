package org.batfish.mulgraph;

import java.util.HashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.DeviceType;
import org.batfish.datamodel.IpWildcard;

import org.batfish.symbolic.Graph;
import org.batfish.symbolic.GraphEdge;
import org.batfish.symbolic.Protocol;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.expr.LiteralInt;
import org.batfish.datamodel.routing_policy.expr.LiteralLong;
import org.batfish.datamodel.routing_policy.statement.Statement;
import org.batfish.datamodel.routing_policy.statement.If;
import org.batfish.datamodel.routing_policy.statement.SetLocalPreference;
import org.batfish.datamodel.routing_policy.statement.SetWeight;
import org.batfish.datamodel.routing_policy.statement.SetMetric;
import org.batfish.datamodel.routing_policy.statement.SetAdministrativeCost;

import org.batfish.datamodel.routing_policy.expr.MatchPrefixSet;
import org.batfish.datamodel.routing_policy.expr.NamedPrefixSet;
import org.batfish.datamodel.RouteFilterList;
import org.batfish.datamodel.RouteFilterLine;
import org.batfish.datamodel.LineAction;

import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.acl.MatchHeaderSpace;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.IpWildcardIpSpace;
import org.batfish.datamodel.Prefix;


import org.batfish.datamodel.routing_policy.expr.Conjunction;
import org.batfish.datamodel.routing_policy.expr.CallExpr;
import org.batfish.datamodel.routing_policy.expr.BooleanExpr;
import org.batfish.datamodel.routing_policy.expr.Disjunction;
import org.batfish.datamodel.routing_policy.expr.Conjunction;
import org.batfish.datamodel.routing_policy.expr.WithEnvironmentExpr;

import org.batfish.datamodel.StaticRoute;

import org.batfish.symbolic.AstVisitor;

import java.util.concurrent.ConcurrentHashMap;

public class buildRag implements Runnable {
	
	Graph g;
	Rag rpg;

	Map<String, List<Protocol>> _protocols;
    private Map<String, RagNode> multigraphNode;
    private Map<String, Set<RagNode>> phyNodeMap;

    IpWildcard srcIp;
    IpWildcard dstIp;

    public RagNode srcTC = null;
    public RagNode dstTC = null;

    String srcNodeName = null;
    String dstNodeName = null;

    long generateTime = 0;

    int countmap = 0;

    boolean correctSrcDst = false;

    public buildRag(Graph g, IpWildcard src, IpWildcard dst) {
        this.g = g;
        rpg = new Rag();
        srcIp = src;
        dstIp = dst;
        multigraphNode = new HashMap<>();
        phyNodeMap = new HashMap<>();
    }

    public buildRag(Graph g, String src, String dst, IpWildcard srcip, IpWildcard dstip) {
        this.g = g;
        rpg = new Rag();
        srcNodeName = src;
        dstNodeName = dst;
        srcIp = srcip;
        dstIp = dstip;
        multigraphNode = new HashMap<>();
        phyNodeMap = new HashMap<>();
    }

    public Rag getRag() {
    	return rpg;
    }

    public long returnGenerateTime() {
        return generateTime;
    }

    public void buildGraph() {
        //long startTime = System.nanoTime();
        buildRouterProtocol();
        initialize();
    	buildRagNodes();
    	buildRagEdges();
        //long endTime = System.nanoTime();
        setNodes();
        //generateTime = endTime - startTime;
        //System.out.println(Rag);
        //System.out.println("src: " + srcTC + "dst: " + dstTC);
        //System.out.println("Generate Time: " + generateTime/1000000 + " ms");
    }

    public void run() {
        buildGraph();
        rpg.taint();
        //draw();
        //printtaint();
        /*
        System.out.println("Number of nodes: " + rpg.getVertices().size());
        System.out.println("Number of edges: " + rpg.getNumberOfEdges());
        System.out.println("Total edgecost var: " + rpg.getNumberOfEdges() * 7);
        */

    }

    public void draw() {
        System.out.println("Edges");
        for ( RagNode r : rpg.getNeighborMap().keySet()) {
            for ( RagEdge e : rpg.getNeighborMap().get(r)) {
                System.out.println(e.getSrc().getId() + "\t" + e.getDst().getId());
            }
        }
    }

    public void printtaint() {
        System.out.println("Taint information");
        for ( RagNode r : rpg.getNeighborMap().keySet()) {
            System.out.println(r.getId() + "\t" + r.getTaint());
        }
    }

    public void initialize() {

        // create physical router to logical router-process mapping
        //System.out.println("Original Routers");
        for (Entry<String, List<GraphEdge>> entry : g.getEdgeMap().entrySet()) {
            String router = entry.getKey();
            phyNodeMap.put(router, new HashSet<RagNode>());
        }

    }



    public void setNodes() {

        //System.out.println(srcNodeName + "\t" + dstNodeName);

        if (srcNodeName == null || srcNodeName == "" || dstNodeName == null || dstNodeName == "") {
            for (Entry<String, List<GraphEdge>> entry : g.getEdgeMap().entrySet()) {
                String router = entry.getKey();
                Configuration conf = g.getConfigurations().get(router);
                for (Protocol proto : _protocols.get(router)) {
                    Set<Prefix> prefixes = Graph.getOriginatedNetworks(conf, proto);
                    //System.out.println(router + "\t" + prefixes);
                    for (Prefix pp : prefixes) {
                        if (pp.containsPrefix(srcIp.toPrefix())) {
                            srcNodeName = router;
                        }
                        if (pp.containsPrefix(dstIp.toPrefix())) {
                            dstNodeName = router;
                        }
                    }
                }
            }
        }
        //System.out.println(phyNodeMap);
        if (srcNodeName != null && phyNodeMap.containsKey(srcNodeName)) {
            //System.out.println(srcNodeName);
            srcTC = new RagNode(srcNodeName, protocol.SRC);
            rpg.add(srcTC);
            Set<RagNode> allnode = phyNodeMap.get(srcNodeName);
            for (RagNode anode : allnode) {
                //rpg.add(srcTC, anode, protocol.SRC);
                rpg.add(anode, srcTC, protocol.SRC);
            }
        }
        if (dstNodeName != null && phyNodeMap.containsKey(dstNodeName)) {
            //System.out.println(dstNodeName);
            dstTC = new RagNode(dstNodeName, protocol.DST);
            rpg.add(dstTC);
            Set<RagNode> allnode = phyNodeMap.get(dstNodeName);
            for (RagNode anode : allnode) {
                //rpg.add(anode, dstTC, protocol.DST);
                rpg.add(dstTC, anode, protocol.DST);
            }

        }
        correctSrcDst = false;
        if (srcTC!=null && dstTC!=null) {
            rpg.setSourceDest(srcTC, dstTC);
            correctSrcDst = true;
        }
    }

    public boolean blockFilter(RoutingPolicy rp, Configuration conf) {
        if (rp == null)
            return false;
        for ( Statement st : rp.getStatements() ) {
            if ( st instanceof If ) {
                If i = (If) st;
                if (i.getGuard() instanceof MatchPrefixSet) {
                    MatchPrefixSet m = (MatchPrefixSet) i.getGuard();
                    if (m.getPrefixSet() instanceof NamedPrefixSet) {
                        NamedPrefixSet x = (NamedPrefixSet) m.getPrefixSet();
                        RouteFilterList fl = conf.getRouteFilterLists().get(x.getName());
                        if (fl != null) {
                            for ( RouteFilterLine line : fl.getLines() ) {
                                if (line.getAction() == LineAction.DENY && line.getIpWildcard().intersects(dstIp))
                                    return true;
                            }

                        }
                    }
                }
            }
        }
        return false;
    }

    public void buildRagEdges() {

    	RagNode src, dst;
    	String srcnode, dstnode, srcname, dstname;

		// create edges connecting routing processes
    	for (Entry<String, List<GraphEdge>> entry : g.getEdgeMap().entrySet()) {
    		String router = entry.getKey();
    		List<GraphEdge> edges = entry.getValue();
    		Configuration conf = g.getConfigurations().get(router);

    		for (Protocol proto : _protocols.get(router)) {
    			for (GraphEdge e : edges) {
    				if (g.isEdgeUsed(conf, proto, e)) {
						srcnode = e.getRouter();
						dstnode = e.getPeer();

						if (srcnode == null || dstnode == null)
							continue;
    					if (proto.isBgp()) {
                            RoutingPolicy importRP = g.findImportRoutingPolicy(router, proto, e);
                            RoutingPolicy exportRP = g.findExportRoutingPolicy(router, proto, e);
                            /*
                            if (importRP != null)
                                System.out.println(router + "\tImport\t" + importRP.getStatements());
                            if (exportRP != null)
                                System.out.println(router + "\tExport\t" + exportRP.getStatements());*/
                            if(blockFilter(importRP, conf) || blockFilter(exportRP, conf)) {
                                //System.out.print("Filter blocks it");
                                continue;
                            }
                            EdgeCost ec = new EdgeCost();
                            srcnode = bgpName(srcnode);
                            dstnode = bgpName(dstnode);
                            ec.setAS(1);
              				src = multigraphNode.get(srcnode);
    						dst = multigraphNode.get(dstnode);
                            if (g.getIbgpNeighbors().containsKey(e)) {
                                rpg.add(src, dst, protocol.IBGP);
                            } else {
                                rpg.add(src, dst, protocol.BGP);
                            }
                            
    					} else if (proto.isOspf()) {
    						srcnode = ospfName(srcnode);
    						dstnode = ospfName(dstnode);
							src = multigraphNode.get(srcnode);
    						dst = multigraphNode.get(dstnode);
    						rpg.add(src, dst, protocol.OSPF);
                        }
    				}
    			}
    		}
    	}


    	// add edges representing redistribution
    	for (Entry<String, Configuration> entry : g.getConfigurations().entrySet()) {
    		String router = entry.getKey();
    		Configuration conf = entry.getValue();
    		for (Protocol proto : _protocols.get(router)) {
                protocol srcType = protocol.NONE;
                if (proto.isBgp()) {
                    srcnode = bgpName(router);
                    srcType = protocol.BGP;
                }
                else if (proto.isOspf()) {
                    srcnode = ospfName(router);
                    srcType = protocol.OSPF;
                } else {
                    continue;
                }
    			src = multigraphNode.get(srcnode);
    			RoutingPolicy pol = Graph.findCommonRoutingPolicy(conf, proto);
    			if (pol == null) {
    				continue;
    			}
    			Set<Protocol> ps = g.findRedistributedProtocols(conf, pol, proto);

    			for (Protocol p : ps) {
                    protocol dstType = protocol.NONE;
                    if (p.isBgp()) {
                        dstnode = bgpName(router);
                        dstType = protocol.BGP;
                    }
                    else if (p.isOspf()) {
                        dstnode = ospfName(router);
                        dstType = protocol.OSPF;
                    } else {
                        continue;
                    }

	    			dst = multigraphNode.get(dstnode);
                    if (src == null || dst == null) {
                            continue;
                    }
					rpg.add(src, dst, protocol.BGP);
    			}
    		}
    	}
    }

    public void buildRagNodes() {

    	RagNode src = null, dst = null;
    	String srcnode, dstnode, srcname, dstname;

        Map<String, Configuration> allConf = g.getConfigurations();
        RagNode protNode = null;
        String protName = null;
        for (String router : allConf.keySet()) {
            Configuration conf = g.getConfigurations().get(router);
            if (conf.getDeviceType() == DeviceType.SWITCH) {
                continue;
            }
            for (Protocol proto : _protocols.get(router)) {

                if (proto.isBgp()) {
                    protName = bgpName(router);
                    protNode = new RagNode(protName, protocol.BGP);
                    rpg.add(protNode);
                    multigraphNode.put(protName, protNode);
                    phyNodeMap.get(router).add(protNode);
                } else if (proto.isOspf()) {
                    protName = ospfName(router);
                    protNode = new RagNode(protName, protocol.OSPF);
                    rpg.add(protNode);
                    multigraphNode.put(protName, protNode);
                    phyNodeMap.get(router).add(protNode);

                }
            }
        }
    }

    public String bgpName(String router) {
        return router+"-BGP";
    }

    public String ospfName(String router) {
        return router+"-OSPF";
    }

    public void buildRouterProtocol() {
    	// create physical router to protocols mapping
    	_protocols = new HashMap<>();
    	g.getConfigurations().forEach((router, conf) -> _protocols.put(router, new ArrayList<>()));
    	g.getConfigurations()
    	.forEach(
    		(router, conf) -> {
              List<Protocol> protos = _protocols.get(router);
              if (conf.getDefaultVrf().getOspfProcess() != null) {
                protos.add(Protocol.OSPF);
              }
              if (conf.getDefaultVrf().getBgpProcess() != null) {
                protos.add(Protocol.BGP);
              }
              if (!conf.getDefaultVrf().getStaticRoutes().isEmpty()) {
                protos.add(Protocol.STATIC);
              }
        });
    }
}
