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

public class buildTpg implements Runnable {
	
	Graph g;
	Tpg tpg;
    buildRag ragBuild;
    Rag rpg;

	Map<String, List<Protocol>> _protocols;
    private Map<String, TpgNode> multigraphNode;
    private Map<String, Set<TpgNode>> phyNodeMap;
    private Map<String, Set<TpgEdge>> phyEdgeMap;

    IpWildcard srcIp;
    IpWildcard dstIp;

    public TpgNode srcTC = null;
    public TpgNode dstTC = null;

    String srcNodeName = null;
    String dstNodeName = null;

    long generateTime = 0;

    int countmap = 0;

    ConcurrentHashMap<String, Tpg> conMap = null;

    boolean correctSrcDst = false;

    public buildTpg(Graph g, IpWildcard src, IpWildcard dst,
     ConcurrentHashMap<String, Tpg> conncMap) {
        this.g = g;
        tpg = new Tpg();
        srcIp = src;
        dstIp = dst;
        multigraphNode = new HashMap<>();
        phyNodeMap = new HashMap<>();
        phyEdgeMap = new HashMap<>();
        ragBuild = new buildRag(g, srcIp, dstIp);
        conMap = conncMap;
    }

    public buildTpg(Graph g, String src, String dst, IpWildcard srcip, IpWildcard dstip,
     ConcurrentHashMap<String, Tpg> conncMap) {
        this.g = g;
        tpg = new Tpg();
        srcNodeName = src;
        dstNodeName = dst;
        srcIp = srcip;
        dstIp = dstip;
        multigraphNode = new HashMap<>();
        phyNodeMap = new HashMap<>();
        phyEdgeMap = new HashMap<>();
        ragBuild = new buildRag(g, srcNodeName, dstNodeName, srcIp, dstIp);
        conMap = conncMap;
    }

    public Tpg getTpg() {
    	return tpg;
    }

    public long returnGenerateTime() {
        return generateTime;
    }

    public void buildGraph() {
        //long startTime = System.nanoTime();
        ragBuild.run();
        rpg = ragBuild.getRag();
        buildRouterProtocol();
        initialize();
    	buildTpgNodes();
    	buildTpgEdges();
        //long endTime = System.nanoTime();
        setNodes();
        tpg.setPhysicalMap(phyEdgeMap);
        String key = srcIp.toString() + "-" + dstIp.toString();
        if (conMap != null && correctSrcDst == true) {
            conMap.put(key, tpg);
        }

        //generateTime = endTime - startTime;
        //System.out.println(Tpg);
        //System.out.println("src: " + srcTC + "dst: " + dstTC);
        //System.out.println("Generate Time: " + generateTime/1000000 + " ms");
    }

    public void run() {
        buildGraph();
        //draw();
        /*
        System.out.println("Number of nodes: " + tpg.getVertices().size());
        System.out.println("Number of edges: " + tpg.getNumberOfEdges());
        System.out.println("Total edgecost var: " + tpg.getNumberOfEdges() * 7);
        */

    }
    public void draw() {
        System.out.println("Edges");
        for ( TpgNode n : tpg.getNeighborMap().keySet()) {
            System.out.println(n+"\t"+tpg.getNeighborMap().get(n));
        }
        /*
        System.out.println("Edges");
        for ( TpgNode r : tpg.getNeighborMap().keySet()) {
            for ( TpgEdge e : tpg.getNeighborMap().get(r)) {
                System.out.println(e.getSrc().getId() + "\t" + e.getDst().getId());
            }
        }
        */
    }

    public void initialize() {

        // create physical router to logical router-process mapping
        //System.out.println("Original Routers");
        for (Entry<String, List<GraphEdge>> entry : g.getEdgeMap().entrySet()) {
            String router = entry.getKey();
            phyNodeMap.put(router, new HashSet<TpgNode>());
        }

    }

    public void buildTpgNodes() {

        Map<String, Configuration> allConf = g.getConfigurations();
        TpgNode protNode = null;
        String protName = null;
        for (String router : allConf.keySet()) {
            Configuration conf = g.getConfigurations().get(router);
            if (conf.getDeviceType() == DeviceType.SWITCH) {
                continue;
            }
            protName = createName(router, "RIB");
            protNode = new TpgNode(protName, protocol.RIB, router);
            tpg.add(protNode);
            multigraphNode.put(protName, protNode);
            phyNodeMap.get(router).add(protNode);

            if (_protocols.get(router).contains(Protocol.OSPF)) {
                protName = createName(router, "OSPF");
                protNode = new TpgNode(protName, protocol.OSPF, router);
                tpg.add(protNode);
                multigraphNode.put(protName, protNode);
                phyNodeMap.get(router).add(protNode);
            }
            if (_protocols.get(router).contains(Protocol.BGP)) {
                protName = createName(router, "BGP");
                protNode = new TpgNode(protName, protocol.BGP, router);
                tpg.add(protNode);
                multigraphNode.put(protName, protNode);
                phyNodeMap.get(router).add(protNode);
            }
        }
        //System.out.println("Edges interface");
        for (Entry<String, List<GraphEdge>> entry : g.getEdgeMap().entrySet()) {
            String router = entry.getKey();
            List<GraphEdge> edges = entry.getValue();
            Configuration conf = g.getConfigurations().get(router);

            //System.out.println("VRF " + conf.getVrfs());
            for (GraphEdge e : edges) {
                //System.out.println(e.getStart()+"\t"+e.getEnd());
                if (g.isEdgeUsed(conf, Protocol.BGP, e)) {
                    if (e.getStart()!=null && e.getEnd()!=null) {
                        protName = bgpName(e.getRouter(), e.getPeer());
                        protNode = new TpgNode(protName, protocol.BGP, e.getRouter());
                        tpg.add(protNode);
                        multigraphNode.put(protName, protNode);
                        phyNodeMap.get(router).add(protNode);
                    }
                }
                if (e.getStart()!=null) {
                    protName = intfName(e.getRouter(), e.getStart().getName());
                    addInterface(e.getRouter(), protName);
                }
                if (e.getEnd()!=null) {
                    protName = intfName(e.getPeer(), e.getEnd().getName());
                    addInterface(e.getPeer(), protName);
                }
            }
        }
        
    }

    public void buildTpgEdges() {

        //TODO: ADD EDGE FROM RIB CORRECTLY BASED ON TAINTS
        String protName = null, protName1 = null, protName2 = null;
        for (Entry<String, List<GraphEdge>> entry : g.getEdgeMap().entrySet()) {
            String router = entry.getKey();
            List<GraphEdge> edges = entry.getValue();
            Configuration conf = g.getConfigurations().get(router);

            //System.out.println("VRF " + conf.getVrfs());
            for (GraphEdge e : edges) {
                //System.out.println(e.getStart()+"\t"+e.getEnd());
                if (e.getStart()!=null) {
                    protName = intfName(e.getRouter(), e.getStart().getName());
                    addIntraIntfEdge(protName);
                    addTpgEdge(protName+"_I", createName(router, "RIB"), protocol.RIB);

                    if (g.isEdgeUsed(conf, Protocol.OSPF, e)) {
                        EdgeCost ec = new EdgeCost();
                        ec.setOSPF(e.getEnd().getOspfCost());
                        addTpgEdge(createName(router, "OSPF"), protName+"_O", protocol.OSPF, ec);    
                    }

                }/*
                if (e.getEnd()!=null) {
                    protName = intfName(e.getPeer(), e.getEnd().getName());
                    addIntraIntfEdge(protName);
                    //addTpgEdge(protName+"_I", createName(router, "RIB"), protocol.RIB);
                }*/
                if (e.getStart()!=null && e.getEnd()!=null) {
                    protName1 = intfName(e.getRouter(), e.getStart().getName());
                    protName2 = intfName(e.getPeer(), e.getEnd().getName());
                    boolean acl = blockAcl(e.getEnd(), e.getStart());
                    addInterIntfEdge(protName1, protName2, e.getRouter(), e.getPeer(), acl);

                    if (g.isEdgeUsed(conf, Protocol.BGP, e)) {

                        RoutingPolicy importRP = g.findImportRoutingPolicy(router, Protocol.BGP, e);
                        RoutingPolicy exportRP = g.findExportRoutingPolicy(router, Protocol.BGP, e);
                        
                        //if (importRP != null)
                        //    System.out.println(router + "\tImport\t" + importRP.getStatements());
                        //if (exportRP != null)
                        //    System.out.println(router + "\tExport\t" + exportRP.getStatements());

                        if(blockFilter(importRP, conf) || blockFilter(exportRP, conf)) {
                            //System.out.print("Filter blocks it");
                            continue;
                        }

                        protName1 = bgpName(e.getRouter(), e.getPeer());
                        protName2 = intfName(e.getRouter(), e.getStart().getName())+"_O";
                        EdgeCost ec = new EdgeCost();
                        setBGPCost(importRP, exportRP, ec, conf);

                        addTpgEdge(createName(router, "BGP"), protName1, protocol.BGP, ec);
                        if (g.getIbgpNeighbors().containsKey(e)) {
                            //System.out.println("iBGP " + e);
                            addDependenceGraph(e);
                            String prefixOspf = intfName(e.getRouter(), e.getStart().getName()) + '_' + createName(router, "OSPF"); 
                            addTpgEdge(protName1, prefixOspf, protocol.IBGP, ec);
                        } else {
                            addTpgEdge(protName1, protName2, protocol.BGP, ec);
                        }

                    } else if (g.isEdgeUsed(conf, Protocol.STATIC, e)) {
                        //skipping static routes to unreachable IP
                        protName1 = createName(e.getRouter(), "RIB");
                        protName2 = intfName(e.getRouter(), e.getStart().getName())+"_O";
                        addTpgEdge(protName1, protName2, protocol.STAT);
                    }
                }
            }
        }

        Map<String, Configuration> allConf = g.getConfigurations();
        for (String router : allConf.keySet()) {
            String ribRouter = createName(router, "RIB");
            if (_protocols.get(router).contains(Protocol.OSPF)) {
                String ospfRouter = createName(router, "OSPF");
                if (rpg.getVertex(ospfRouter) != null && rpg.getVertex(ospfRouter).getTaint())
                    addTpgEdge(ribRouter, ospfRouter, protocol.OSPF);
            }
            if (_protocols.get(router).contains(Protocol.BGP)) {
                String bgpRouter = createName(router, "BGP");
                if (rpg.getVertex(bgpRouter) != null && rpg.getVertex(bgpRouter).getTaint())
                    addTpgEdge(ribRouter, bgpRouter, protocol.BGP);
            }
        }

    }

    public void addDependenceGraph(GraphEdge depEdge) {

        String prefix = intfName(depEdge.getRouter(), depEdge.getStart().getName());

        String protName = null, protName1 = null, protName2 = null;
        TpgNode protNode = null;
        //System.out.println("Prefix\t" + prefix + "\t" + depEdge.getStart().getName() + "\t" + depEdge.getEnd().getName());
        //creating all relevant nodes
        //System.out.println("#################NODES        CREATED########################");
        for (Entry<String, List<GraphEdge>> entry : g.getEdgeMap().entrySet()) {
            String router = entry.getKey();
            List<GraphEdge> edges = entry.getValue();
            Configuration conf = g.getConfigurations().get(router);

            if (_protocols.get(router).contains(Protocol.OSPF)) {
                protName = prefix + '_' + createName(router, "OSPF");
                protNode = new TpgNode(protName, protocol.OSPF, router);
                //System.out.println(protName);
                tpg.add(protNode);
                multigraphNode.put(protName, protNode);
                phyNodeMap.get(router).add(protNode);
            }
            //System.out.println("VRF " + conf.getVrfs());
            for (GraphEdge e : edges) {
                if (g.isEdgeUsed(conf, Protocol.BGP, e)) {
                    continue;
                }
                if (e.getStart()!=null && e.getEnd()!=null) {
                    protName = prefix + '_' + intfName(e.getRouter(), e.getStart().getName());
                    addInterface(e.getRouter(), protName);
                    //System.out.println(protName+"\tIO");
                    //protName = prefix + '_' + intfName(e.getPeer(), e.getEnd().getName());
                    //addInterface(e.getPeer(), protName);
                }
            }
        }
        //System.out.println("#########################################");
        // create all relevant edges
        for (Entry<String, List<GraphEdge>> entry : g.getEdgeMap().entrySet()) {
            String router = entry.getKey();
            List<GraphEdge> edges = entry.getValue();
            Configuration conf = g.getConfigurations().get(router);

            for (GraphEdge e : edges) {
                if (e.getStart()!=null && e.getEnd()!=null) {
                    if (g.isEdgeUsed(conf, Protocol.BGP, e)) {
                        continue;
                    }
                    protName1 = prefix + '_' + intfName(e.getRouter(), e.getStart().getName());
                    protName2 = prefix + '_' + intfName(e.getPeer(), e.getEnd().getName());
                    addIntraIntfEdge(protName1);
                    addIntraIntfEdge(protName2);
                    boolean acl = blockAcl(e.getEnd(), e.getStart());
                    addInterIntfEdge(protName1, protName2, e.getRouter(), e.getPeer(), acl);

                    //exit edges
                    //System.out.println("@@\t"+e.getPeer()+"\t"+depEdge.getPeer());
                    if ( e.getPeer().equals(depEdge.getPeer())) {
                        //System.out.println("##\t"+protName2);
                        addTpgEdge(protName2+"_I", createName(depEdge.getPeer(), "BGP"), protocol.BGP);                
                    }


                    if (g.isEdgeUsed(conf, Protocol.OSPF, e)) {
                        EdgeCost ec = new EdgeCost();
                        ec.setOSPF(e.getEnd().getOspfCost());                        
                        addTpgEdge(prefix + '_' + createName(router, "OSPF"), protName1+"_O", protocol.OSPF, ec);    
                        continue;
                    }
                }
            }
        }
        /*
        System.out.println("Exit edge\t"+intfName(depEdge.getPeer(), depEdge.getEnd().getName()));
        protName2 = prefix + '_' + intfName(depEdge.getPeer(), depEdge.getEnd().getName()) + "_I";
        addTpgEdge(protName2, createName(depEdge.getPeer(), "BGP"), protocol.BGP);         
        */
    }

    public void addTpgEdge(String name1, String name2, protocol prot) {
        TpgNode in = multigraphNode.get(name1);
        TpgNode out = multigraphNode.get(name2);
        if(in==null || out==null) {
            //System.out.println("addTpgEdge Null val " + name1 + "\t" + name2 + "\t"  + in + "\t"  + out);
            return;
        }
        tpg.add(in, out, new EdgeCost(), prot);
    }

    public void addTpgEdge(String name1, String name2, protocol prot, EdgeCost ec) {
        TpgNode in = multigraphNode.get(name1);
        TpgNode out = multigraphNode.get(name2);
        if(in==null || out==null) {
            //System.out.println("addTpgEdge Null val " + name1 + "\t" + name2 + "\t"  + in + "\t"  + out);
            return;
        }
        tpg.add(in, out, ec, prot);
    }

    public void addIntraIntfEdge(String name) {
        TpgNode in = multigraphNode.get(name+"_I");
        TpgNode out = multigraphNode.get(name+"_O");
        if(in==null || out==null) {
            //System.out.println("addIntraIntfEdge Null val");
            return;
        }
        tpg.add(in, out, new EdgeCost(), protocol.INTF);
    }

    public void addInterIntfEdge(String name1, String name2, String srcRouter, String dstRouter, boolean acl) {
        TpgNode in = multigraphNode.get(name1+"_O");
        TpgNode out = multigraphNode.get(name2+"_I");
        if(in==null || out==null) {
            //System.out.println("addInterIntfEdge Null val");
            return;
        }
        String key = srcRouter + "_" + dstRouter;
        TpgEdge thisEdge = tpg.add(in, out, new EdgeCost(), protocol.INTF);

        if (acl) {
            thisEdge.setACL();
        }

        thisEdge.setRemovable();

        if (!phyEdgeMap.containsKey(key)) {
            phyEdgeMap.put(key, new HashSet<>());
        }
        phyEdgeMap.get(key).add(thisEdge);
    }


    public void addInterface(String router, String protName) {
        String inName = protName+"_I";

        if (multigraphNode.containsKey(inName))
            return;
        TpgNode protNode = new TpgNode(inName, protocol.INTF, router);
        tpg.add(protNode);
        multigraphNode.put(inName, protNode);
        phyNodeMap.get(router).add(protNode);                    

        String outName = protName+"_O";
        protNode = new TpgNode(outName, protocol.INTF, router);
        tpg.add(protNode);
        multigraphNode.put(outName, protNode);
        phyNodeMap.get(router).add(protNode);                    
    }

    public String createName(String router, String prefix) {
        return router+"-"+prefix;
    }

    public String bgpName(String router) {
        return router+"-BGP";
    }

    public String bgpName(String router, String next) {
        return router+"-BGP_"+next;
    }

    public String intfName(String router, String intf) {
        return router+"_"+intf;
    }

    public String ospfName(String router) {
        return router+"-OSPF";
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

        if (srcNodeName != null && phyNodeMap.containsKey(srcNodeName)) {
            //System.out.println(srcNodeName);
            srcTC = new TpgNode(srcNodeName, protocol.SRC, srcNodeName);
            tpg.add(srcTC);
            String ribSrc = createName(srcNodeName, "RIB");
            TpgNode ribSrcNode = tpg.getVertex(ribSrc);
            if ( ribSrcNode != null) {
                tpg.add(srcTC, ribSrcNode, new EdgeCost(), protocol.SRC);
            } else {
                //System.out.println("Invalid Source");
            }

            /*
            String ospfRouter = createName(srcNodeName, "OSPF");
            String bgpRouter = createName(srcNodeName, "BGP");

            TpgNode ospfNode = tpg.getVertex(ospfRouter);
            if (ospfNode != null) {
                tpg.add(ospfNode, srcTC, new EdgeCost(), protocol.SRC);
            }
            TpgNode bgpNode = tpg.getVertex(bgpRouter);
            if (bgpNode != null) {
                tpg.add(bgpNode, srcTC, new EdgeCost(), protocol.SRC);
            }*/
        }
        if (dstNodeName != null && phyNodeMap.containsKey(dstNodeName)) {
            //System.out.println(dstNodeName);
            dstTC = new TpgNode(dstNodeName, protocol.DST, dstNodeName);
            tpg.add(dstTC);
            String ribDst = createName(dstNodeName, "RIB");
            TpgNode ribDstNode = tpg.getVertex(ribDst);
            if ( ribDstNode != null) {
                tpg.add( ribDstNode, dstTC, new EdgeCost(), protocol.DST);
            } else {
                //System.out.println("Invalid Destination");
            }            
            /*
            String ospfRouter = createName(dstNodeName, "OSPF");
            String bgpRouter = createName(dstNodeName, "BGP");

            TpgNode ospfNode = tpg.getVertex(ospfRouter);
            if (ospfNode != null) {
                tpg.add(dstTC, ospfNode, new EdgeCost(), protocol.DST);
            }
            TpgNode bgpNode = tpg.getVertex(bgpRouter);
            if (bgpNode != null) {
                tpg.add(dstTC, bgpNode, new EdgeCost(), protocol.DST);
            }
            */
        }
        correctSrcDst = false;
        if (srcTC!=null && dstTC!=null) {
            tpg.setSourceDest(srcTC, dstTC);
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

    public boolean blockAcl(Interface out, Interface in) {
        //Interface i = ge.getStart();

        IpAccessList outbound = out.getOutgoingFilter();

        if (outbound != null) {
            for ( IpAccessListLine line : outbound.getLines() ) {
                if (line.getAction() == LineAction.DENY) {
                    if ( line.getMatchCondition() instanceof MatchHeaderSpace) {
                        MatchHeaderSpace mhs = (MatchHeaderSpace)line.getMatchCondition();

                        if (mhs.getHeaderspace().getSrcIps() instanceof IpWildcardIpSpace &&
                            mhs.getHeaderspace().getDstIps() instanceof IpWildcardIpSpace) {
                            IpWildcardIpSpace srcWildCard = (IpWildcardIpSpace)mhs.getHeaderspace().getSrcIps();
                            IpWildcardIpSpace dstWildCard = (IpWildcardIpSpace)mhs.getHeaderspace().getDstIps();
                            if (srcWildCard.getIpWildcard().intersects(srcIp) && 
                                dstWildCard.getIpWildcard().intersects(dstIp)) {
                                //System.out.println("Blocked by ACL " + mhs.getHeaderspace().getSrcIps() + 
                                //    "\t" + mhs.getHeaderspace().getDstIps());
                                return true;
                            }
                        }
                    }
                }
            }
        }

        //Interface i = ge.getEnd();
        IpAccessList inbound = in.getIncomingFilter();

        if (inbound != null) {
            for ( IpAccessListLine line : inbound.getLines() ) {
                if (line.getAction() == LineAction.DENY) {
                    if ( line.getMatchCondition() instanceof MatchHeaderSpace) {
                        MatchHeaderSpace mhs = (MatchHeaderSpace)line.getMatchCondition();

                        if (mhs.getHeaderspace().getSrcIps() instanceof IpWildcardIpSpace &&
                            mhs.getHeaderspace().getDstIps() instanceof IpWildcardIpSpace) {
                            IpWildcardIpSpace srcWildCard = (IpWildcardIpSpace)mhs.getHeaderspace().getSrcIps();
                            IpWildcardIpSpace dstWildCard = (IpWildcardIpSpace)mhs.getHeaderspace().getDstIps();
                            if (srcWildCard.getIpWildcard().intersects(srcIp) && 
                                dstWildCard.getIpWildcard().intersects(dstIp)) {
                                //System.out.println("Blocked by ACL " + mhs.getHeaderspace().getSrcIps() + 
                                //    "\t" + mhs.getHeaderspace().getDstIps());
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    public void setBGPCost(RoutingPolicy importRP, RoutingPolicy exportRP, EdgeCost ec, Configuration conf) {
        setBGPCostPol(importRP, ec, conf);
        setBGPCostPol(exportRP, ec, conf);
    }

    public void setBGPCostPol(RoutingPolicy rp, EdgeCost ec, Configuration conf) {
        if (rp != null) {
            for (Statement st : rp.getStatements()) {
                if ( st instanceof SetLocalPreference ) {
                    SetLocalPreference i = (SetLocalPreference) st;
                    if (i.getLocalPreference() instanceof LiteralInt) {
                        LiteralInt li = (LiteralInt) i.getLocalPreference();
                        ec.setLP(li.getValue());
                    }
                } else if ( st instanceof SetMetric ) {
                    SetMetric i = (SetMetric) st;
                    if (i.getMetric() instanceof LiteralLong) {
                        LiteralLong li = (LiteralLong) i.getMetric();
                        ec.setMED(((int)li.getValue()));
                    }
                }  else if ( st instanceof SetWeight ) {
                    SetWeight i = (SetWeight) st;
                    if (i.getWeight() instanceof LiteralInt) {
                        LiteralInt li = (LiteralInt) i.getWeight();
                        ec.setWeight(li.getValue());
                    }
                } else if ( st instanceof SetAdministrativeCost ) {
                    SetAdministrativeCost i = (SetAdministrativeCost) st;
                    if (i.getAdmin() instanceof LiteralInt) {
                        LiteralInt li = (LiteralInt) i.getAdmin();
                        ec.setAD(li.getValue());
                    }
                } else if ( st instanceof If ) {
                    If i = (If) st;
                    //System.out.println(i.getGuard()+"\t"+i.getTrueStatements()+"\t"+i.getFalseStatements());
                    if (i.getGuard() instanceof MatchPrefixSet) {
                        MatchPrefixSet m = (MatchPrefixSet) i.getGuard();
                        if (m.getPrefixSet() instanceof NamedPrefixSet) {
                            NamedPrefixSet x = (NamedPrefixSet) m.getPrefixSet();
                            RouteFilterList fl = conf.getRouteFilterLists().get(x.getName());
                            if (matches(fl)) {
                                for ( Statement trueStmt : i.getTrueStatements()) {
                                    if ( trueStmt instanceof SetLocalPreference ) {
                                        SetLocalPreference lp = (SetLocalPreference) trueStmt;
                                        if (lp.getLocalPreference() instanceof LiteralInt) {
                                            LiteralInt li = (LiteralInt) lp.getLocalPreference();
                                            ec.setLP(li.getValue());
                                        }
                                    } else if ( trueStmt instanceof SetMetric ) {
                                        SetMetric lmetric = (SetMetric) trueStmt;
                                        if (lmetric.getMetric() instanceof LiteralLong) {
                                            LiteralLong li = (LiteralLong) lmetric.getMetric();
                                            ec.setMED(((int)li.getValue()));
                                        }
                                    }  else if ( trueStmt instanceof SetWeight ) {
                                        SetWeight lweight = (SetWeight) trueStmt;
                                        if (lweight.getWeight() instanceof LiteralInt) {
                                            LiteralInt li = (LiteralInt) lweight.getWeight();
                                            ec.setWeight(li.getValue());
                                        }
                                    }  else if ( trueStmt instanceof SetAdministrativeCost ) {
                                        SetAdministrativeCost lad = (SetAdministrativeCost) trueStmt;
                                        if (lad.getAdmin() instanceof LiteralInt) {
                                            LiteralInt li = (LiteralInt) lad.getAdmin();
                                            ec.setAD(li.getValue());
                                        }
                                    }
                                }
                            }
                        }

                    }
                }
            }

        }
    }

    public boolean matches(RouteFilterList fl) {
        if (fl != null) {
            for ( RouteFilterLine line : fl.getLines() ) {
                if (line.getIpWildcard().intersects(dstIp))
                    return true;
            }
        }
        return false;
    }


    /*

    public void buildTpgEdges() {

    	TpgNode src, dst;
    	String pro, srcnode, dstnode, srcname, dstname;

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
                        srcname = e.getRouter();
                        dstname = e.getPeer();

						if (srcnode == null || dstnode == null)
							continue;
    					if (proto.isBgp()) {
                            RoutingPolicy importRP = g.findImportRoutingPolicy(router, proto, e);
                            RoutingPolicy exportRP = g.findExportRoutingPolicy(router, proto, e);
                            
                            //if (importRP != null)
                            //    System.out.println(router + "\tImport\t" + importRP.getStatements());
                            //if (exportRP != null)
                            //    System.out.println(router + "\tExport\t" + exportRP.getStatements());

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
                                tpg.add(src, dst, protocol.IBGP);
                            } else {
                                tpg.add(src, dst, protocol.BGP);
                            }
                            
    					} else if (proto.isOspf()) {
    						srcnode = ospfName(srcnode);
    						dstnode = ospfName(dstnode);
							src = multigraphNode.get(srcnode);
    						dst = multigraphNode.get(dstnode);
    						tpg.add(src, dst, protocol.OSPF);
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
                    srcnode = "OSPF";
                    srcType = protocol.OSPF;
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
                    }

	    			dst = multigraphNode.get(dstnode);
                    if (src == null || dst == null) {
                            continue;
                    }
					tpg.add(src, dst, protocol.BGP);
    			}
    		}
    	}
    }

    */
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
