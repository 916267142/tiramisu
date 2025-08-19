package org.batfish.mulgraph;

import static org.batfish.symbolic.CommunityVarCollector.collectCommunityVars;

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


import org.batfish.datamodel.routing_policy.statement.AddCommunity;
import org.batfish.datamodel.routing_policy.statement.DeleteCommunity;
import org.batfish.datamodel.routing_policy.statement.RetainCommunity;
import org.batfish.datamodel.routing_policy.statement.SetCommunity;
import org.batfish.datamodel.routing_policy.expr.MatchCommunitySet;
import org.batfish.datamodel.routing_policy.expr.CommunitySetExpr;
import org.batfish.datamodel.routing_policy.expr.Conjunction;
import org.batfish.datamodel.routing_policy.expr.CallExpr;
import org.batfish.datamodel.routing_policy.expr.BooleanExpr;
import org.batfish.datamodel.routing_policy.expr.Disjunction;
import org.batfish.datamodel.routing_policy.expr.Conjunction;
import org.batfish.datamodel.routing_policy.expr.WithEnvironmentExpr;

import org.batfish.datamodel.CommunityList;
import org.batfish.datamodel.CommunityListLine;
import org.batfish.datamodel.StaticRoute;

import org.batfish.symbolic.AstVisitor;
import org.batfish.symbolic.CommunityVar;

import java.util.concurrent.ConcurrentHashMap;

public class Mulgraph2 implements Runnable {
	
	public Graph g;
	public Digraph dg;

	public Map<String, List<Protocol>> _protocols;
	// Map of vertex-key to multilayer graph node 
	public Map<String, Node> multigraphNode;
    public Map<String, Set<Node>> phyNodeMap;
    public Map<String, Set<Edge>> phyEdgeMap;


    public Map<String, Long> _communityID;
    public Map<String, Set<String>> _actCommunity;
    public Map<String, Set<String>> _addCommunity;
    public Map<String, Set<String>> _removeCommunity;

    public Map<String, Map<String, EdgeCost>> _routerCommunityLp;

    public Map<String, Set<String>> _vrfMap;
    public Set<String> _allVrfMap;

    public Map<String, Map<String, Set<String>>> l2map;

    public Map<String, Map<String, Node>> _switchVLANMap;

    public Set<String> _hasIBGP;

    public Map<Edge, Interface> aclEdgeMapStart;
    public Map<Edge, Interface> aclEdgeMapEnd;
    public Map<Edge, RoutingPolicy> importMap;
    public Map<Edge, RoutingPolicy> exportMap;
    public Map<Edge, String> policyConfMap;

    public Map<Edge, List<StaticRoute>> staticEdgeMap;

    IpWildcard srcIp;
    IpWildcard dstIp;

    public Node srcNode = null;
    public Node dstNode = null;

    String srcName = null;
    String dstName = null;

    long generateTime = 0;

    int countmap = 0;

    ConcurrentHashMap<String, Digraph> conMap = null;

    boolean correctSrcDst = false;

    Mulgraph2 basic;

    public Mulgraph2(Graph g, IpWildcard src, IpWildcard dst) {//, String src, String dst){
        this.g = g;
        dg = new Digraph();
        srcIp = src;
        dstIp = dst;

        multigraphNode = new HashMap<>();
        phyNodeMap = new HashMap<>();
        phyEdgeMap = new HashMap<>();

        _actCommunity = new TreeMap<>();
        _addCommunity = new TreeMap<>();
        _removeCommunity = new TreeMap<>();
        _routerCommunityLp = new TreeMap<>();
        _vrfMap = new TreeMap<>();
        _allVrfMap = new HashSet<>();
        _switchVLANMap = new TreeMap<>();
        _hasIBGP = new HashSet<>();
        aclEdgeMapStart = new HashMap<>();
        aclEdgeMapEnd = new HashMap<>();
        staticEdgeMap = new HashMap<>();
        importMap = new HashMap<>();
        exportMap = new HashMap<>();
        policyConfMap = new HashMap<>();
        buildGraph();

    }

    public Mulgraph2(Graph g, IpWildcard src, IpWildcard dst,
     ConcurrentHashMap<String, Digraph> conncMap) {
        this.g = g;
        dg = new Digraph();
        srcIp = src;
        dstIp = dst;

        multigraphNode = new HashMap<>();
        phyNodeMap = new HashMap<>();
        phyEdgeMap = new HashMap<>();

        _actCommunity = new TreeMap<>();
        _addCommunity = new TreeMap<>();
        _removeCommunity = new TreeMap<>();
        _routerCommunityLp = new TreeMap<>();
        _vrfMap = new TreeMap<>();
        _allVrfMap = new HashSet<>();
        _switchVLANMap = new TreeMap<>();
        _hasIBGP = new HashSet<>();
        aclEdgeMapStart = new HashMap<>();
        aclEdgeMapEnd = new HashMap<>();
        staticEdgeMap = new HashMap<>();
        importMap = new HashMap<>();
        exportMap = new HashMap<>();
        policyConfMap = new HashMap<>();
        conMap = conncMap;
        buildGraph();
    }

    public Mulgraph2(Graph g, String src, String dst, IpWildcard srcip, IpWildcard dstip,
     ConcurrentHashMap<String, Digraph> conncMap) {
        this.g = g;
        dg = new Digraph();
        srcName = src;
        dstName = dst;
        srcIp = srcip;
        dstIp = dstip;

        multigraphNode = new HashMap<>();
        phyNodeMap = new HashMap<>();
        phyEdgeMap = new HashMap<>();

        _actCommunity = new TreeMap<>();
        _addCommunity = new TreeMap<>();
        _removeCommunity = new TreeMap<>();
        _routerCommunityLp = new TreeMap<>();
        _vrfMap = new TreeMap<>();
        _allVrfMap = new HashSet<>();
        _switchVLANMap = new TreeMap<>();
        _hasIBGP = new HashSet<>();
        aclEdgeMapStart = new HashMap<>();
        aclEdgeMapEnd = new HashMap<>();
        staticEdgeMap = new HashMap<>();
        importMap = new HashMap<>();
        exportMap = new HashMap<>();
        policyConfMap = new HashMap<>();
        conMap = conncMap;
        buildGraph();

    }

    public Mulgraph2(Graph g, String src, String dst, IpWildcard srcip, IpWildcard dstip,
     ConcurrentHashMap<String, Digraph> conncMap, Mulgraph2 basicMul) {
        this.g = g;
        dg = new Digraph();
        srcName = src;
        dstName = dst;
        srcIp = srcip;
        dstIp = dstip;
        basic = basicMul;

        _protocols = basic._protocols;
        multigraphNode = basic.multigraphNode;
        phyNodeMap = basic.phyNodeMap;
        phyEdgeMap = new HashMap<>();
        l2map = basic.l2map;
        _switchVLANMap = basic._switchVLANMap;
        _hasIBGP = basic._hasIBGP;
        conMap = conncMap;
        _vrfMap = basic._vrfMap;
        _allVrfMap = basic._allVrfMap;

        _communityID = basic._communityID;
        _actCommunity = basic._actCommunity;
        _addCommunity = basic._addCommunity;
        _removeCommunity = basic._removeCommunity;
        _routerCommunityLp = basic._routerCommunityLp;
        aclEdgeMapStart = basic.aclEdgeMapStart;
        aclEdgeMapEnd = basic.aclEdgeMapEnd;
        staticEdgeMap = basic.staticEdgeMap;
        importMap = basic.importMap;
        exportMap = basic.exportMap;
        policyConfMap = basic.policyConfMap;
    }

    public void setL2Map(Map<String, Map<String, Set<String>>> l2) {
        l2map = l2;
    }

    public Digraph getDigraph() {
    	return dg;
    }

    public long returnGenerateTime() {
        return generateTime;
    }

    public void newBuildGraph() {

        for (Node aNode : basic.dg.getVertices()) {
            dg.add(aNode);
        }

        for (Node aNode : basic.dg.getVertices()) {
            
            for ( Edge thisEdge : basic.dg.getNeighbors(aNode)) {
                //*
                if (staticEdgeMap.containsKey(thisEdge)) {
                    boolean applies = false;
                    for (StaticRoute sr : staticEdgeMap.get(thisEdge)) {
                        if (dstIp.containsIp(sr.getNextHopIp())) {
                            applies = true;
                            int staticWeight = sr.getMetric().intValue();
                            //ec.setWeight(staticWeight);
                            break;
                        }
                    }
                    if (applies == false)
                        continue;

                }//*/
                if (aclEdgeMapStart.containsKey(thisEdge)) {
                    if (blockAcl(aclEdgeMapStart.get(thisEdge), aclEdgeMapEnd.get(thisEdge)))
                        continue;
                }
                if (importMap.containsKey(thisEdge)) {
                    Configuration conf =  g.getConfigurations().get(policyConfMap.get(thisEdge));
                    if (blockFilter(importMap.get(thisEdge), conf) || blockFilter(exportMap.get(thisEdge), conf))
                        continue;
                }
                dg.add(thisEdge.getSrc(), thisEdge.getDst(), thisEdge.getCost(), thisEdge.getType());
            }
        }

        //buildEdges();
        //addDEFEdge();
        setNodes();

        String key = srcIp.toString() + "-" + dstIp.toString();
        if (conMap != null && correctSrcDst == true) {
            conMap.put(key, dg);
        }
    }

    public void buildGraph() {
        initialize();
        //long startTime = System.nanoTime();
        buildVrfMap();
        buildRouterProtocol();
        buildCommunity();
    	buildNodes();
    	buildEdges();
        addDEFEdge();
        //long endTime = System.nanoTime();
        //setNodes();
        //generateTime = endTime - startTime;


        /*
        String key = srcIp.toString() + "-" + dstIp.toString();
        if (conMap != null && correctSrcDst == true) {
            conMap.put(key, dg);
        }*/

        //dg.printAllPhysicalMap();
        //System.out.println("original");
        //System.out.println(dg);
        //System.out.println("#############");
        //System.out.println("src: " + srcNode + "dst: " + dstNode);
        //System.out.println("Generate Time: " + generateTime/1000000 + " ms");
    }

    public void run() {
        newBuildGraph();
        //System.out.println(srcNode + "\t" + dstNode+ "\n" + dg);
        /*
        System.out.println("Number of nodes: " + dg.getVertices().size());
        System.out.println("Number of edges: " + dg.getNumberOfEdges());
        System.out.println("Total community " + countComm());
        System.out.println("Total edgecost var: " + dg.getNumberOfEdges() * 7);
        System.out.println("Num ibgp: " + (dg.iBGPCorr.size() * 2 + dg.defCorr.size() * 2));
        System.out.println("Num ibgp: " + countIBGP());*/

    }

    public int countIBGP(){
        int count = 0;

        for (Node aNode : basic.dg.getVertices()) {            
            for ( Edge thisEdge : basic.dg.getNeighbors(aNode)) {
                if (thisEdge.getType() == protocol.IBGP || thisEdge.getType() == protocol.DEF)
                    count = count + 1;
            }
        }

        return count;
    }

    public int countComm(){
        int count = 0;

        for (Node aNode: dg.getVertices()) {
            count = count + aNode.addedCommunity.size() + aNode.removedCommunity.size()
                + aNode.blockedCommunity.size();
        }

        return count;
    }

    public void initialize() {

        // create VRF map and initialize communitymaps
        for (String router : g.getRouters()) {
            _vrfMap.put(router, new HashSet<>());
            _addCommunity.put(router, new HashSet<>());
            _removeCommunity.put(router, new HashSet<>());
            _actCommunity.put(router, new HashSet<>());                                        
        }

        // create physical router to logical router-process mapping
        //System.out.println("Original Routers");
        for (Entry<String, List<GraphEdge>> entry : g.getEdgeMap().entrySet()) {
            String router = entry.getKey();
            phyNodeMap.put(router, new HashSet<Node>());
        }

    }

    public void setNodes() {

        //System.out.println(srcName + "\t" + dstName);

        if (srcName == null || srcName == "" || dstName == null || dstName == "") {
            for (Entry<String, List<GraphEdge>> entry : g.getEdgeMap().entrySet()) {
                String router = entry.getKey();
                Configuration conf = g.getConfigurations().get(router);
                for (Protocol proto : _protocols.get(router)) {
                    Set<Prefix> prefixes = Graph.getOriginatedNetworks(conf, proto);
                    //System.out.println(router + "\t" + prefixes);
                    for (Prefix pp : prefixes) {
                        if (pp.containsPrefix(srcIp.toPrefix())) {
                            srcName = router;
                        }
                        if (pp.containsPrefix(dstIp.toPrefix())) {
                            dstName = router;
                        }
                    }
                }
            }
        }
        //System.out.println(phyNodeMap);
        if (srcName != null && phyNodeMap.containsKey(srcName)) {
            //System.out.println(srcName);
            srcNode = new Node(srcName, protocol.SRC);
            dg.add(srcNode);
            Set<Node> allnode = phyNodeMap.get(srcName);
            for (Node anode : allnode) {
                dg.add(srcNode, anode, returnDefaultEC(), protocol.SRC);//anode.getType());
            }
        }
        if (dstName != null && phyNodeMap.containsKey(dstName)) {
            //System.out.println(dstName);
            dstNode = new Node(dstName, protocol.DST);
            dg.add(dstNode);
            Set<Node> allnode = phyNodeMap.get(dstName);
            for (Node anode : allnode) {
                EdgeCost thisEC = returnDefaultEC();
                thisEC.AD = Edge.protocol_map.get(protocol.DST);
                dg.add(anode, dstNode, returnDefaultEC(), protocol.DST);//anode.getType());
            }

        }
        correctSrcDst = false;
        if (srcNode!=null && dstNode!=null) {
            dg.setSourceDest(srcNode, dstNode);
            correctSrcDst = true;
        }
    }

    public void addDEFEdge() {
        // add edge between ibgp and ospf
        for (String node : phyNodeMap.keySet()) {
            Set<Node> allnode = phyNodeMap.get(node);
            for (Node srcprot : allnode) {
                if (srcprot.getType() == protocol.OSPF) {
                    for (Node dstprot : allnode) {
                        if (dstprot.getType() == protocol.BGP) {
                            //System.out.println("Mayadd "+ srcprot +"\t"+ dstprot);
                            //if (hasSameVrf(node, srcprot, dstprot))
                                dg.add(srcprot, dstprot, returnDefaultEC(), protocol.DEF);
                            //connectVRF(node, node, srcprot.getId(), dstprot.getId(), ec, false, protocol.IBGP);
                        }
                    }
                }
            }
        }

    }

    public void buildVrfMap() {


        for (Entry<String, List<GraphEdge>> entry : g.getEdgeMap().entrySet()) {
            String router = entry.getKey();

            List<GraphEdge> edges = entry.getValue();
            for (GraphEdge e : edges) {
                boolean addedStart = false, addedEnd = false;
                String startVrfName = "", endVrfName = "";
                String peer = e.getPeer();
                if (e.getStart()!=null) {
                    startVrfName = e.getStart().getVrfName();
                    if (!(startVrfName.equals("default") || startVrfName.equals("Mgmt-intf"))) {
                        _allVrfMap.add(startVrfName);
                        addedStart = true;
                        _vrfMap.get(router).add(startVrfName);
                    }
                }
                if (e.getEnd()!=null) {
                    endVrfName = e.getEnd().getVrfName();
                    if (!(endVrfName.equals("default") || endVrfName.equals("Mgmt-intf"))) {
                        _allVrfMap.add(endVrfName);
                        addedEnd = true;
                        _vrfMap.get(peer).add(endVrfName);
                    }
                }
                if (peer!=null && addedStart == true && addedEnd == false) {
                    _vrfMap.get(peer).add(startVrfName);
                } else if (router!=null && addedEnd == true && addedStart == false) {
                    _vrfMap.get(router).add(endVrfName);
                }

            }
        }

        for (String router : g.getRouters()) {
            if (_vrfMap.get(router).size() == 0 && g.getConfigurations().get(router).getMPLS())
                _vrfMap.get(router).addAll(_allVrfMap);
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

    public boolean matches(RouteFilterList fl) {
        if (fl != null) {
            for ( RouteFilterLine line : fl.getLines() ) {
                if (line.getIpWildcard().intersects(dstIp))
                    return true;
            }
        }
        return false;
    }

    public void setBGPCost(RoutingPolicy importRP, RoutingPolicy exportRP, EdgeCost ec, Configuration conf) {
        setBGPCostPol(importRP, ec, conf);
        setBGPCostPol(exportRP, ec, conf);
    }

    public void setRouterCommunityCost(List<Statement> stmt, String router, String comm) {
        EdgeCost ec = new EdgeCost();
        boolean set = false;
        for (Statement st : stmt) {
            if ( st instanceof SetLocalPreference ) {
                SetLocalPreference i = (SetLocalPreference) st;
                if (i.getLocalPreference() instanceof LiteralInt) {
                    LiteralInt li = (LiteralInt) i.getLocalPreference();
                    ec.setLP(li.getValue());
                    set = true;
                }
            } else if ( st instanceof SetMetric ) {
                SetMetric i = (SetMetric) st;
                if (i.getMetric() instanceof LiteralLong) {
                    LiteralLong li = (LiteralLong) i.getMetric();
                    ec.setMED(((int)li.getValue()));
                    set = true;
                }
            }  else if ( st instanceof SetWeight ) {
                SetWeight i = (SetWeight) st;
                if (i.getWeight() instanceof LiteralInt) {
                    LiteralInt li = (LiteralInt) i.getWeight();
                    ec.setWeight(li.getValue());
                    set = true;
                }
            } else if ( st instanceof SetAdministrativeCost ) {
                SetAdministrativeCost i = (SetAdministrativeCost) st;
                if (i.getAdmin() instanceof LiteralInt) {
                    LiteralInt li = (LiteralInt) i.getAdmin();
                    ec.setAD(li.getValue());
                    set = true;
                }
            }
        }
        if (set)
            _routerCommunityLp.get(router).put(comm, ec);
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


    public void appliesRP(String router, List<Statement> stmts) {

        Configuration conf = g.getConfigurations().get(router);
        for (Statement stmt : stmts) {
            if (stmt instanceof SetCommunity) {
              SetCommunity sc = (SetCommunity) stmt;
              for (CommunityVar cv : collectCommunityVars(conf, sc.getExpr())) {
                _addCommunity.get(router).add( cv.getValue().replace("$","").replace("^","") );
              }
            } else if (stmt instanceof AddCommunity) {
              AddCommunity ac = (AddCommunity) stmt;
              for (CommunityVar cv : collectCommunityVars(conf, ac.getExpr())) {
                _addCommunity.get(router).add( cv.getValue().replace("$","").replace("^","") );
              }
            } else if (stmt instanceof DeleteCommunity) {
              DeleteCommunity dc = (DeleteCommunity) stmt;
              for (CommunityVar cv : collectCommunityVars(conf, dc.getExpr())) {
                _removeCommunity.get(router).add( cv.getValue().replace("$","").replace("^","") );
              }
            } else if ( stmt instanceof If ) {
                If i = (If) stmt;
                // do nothing
                //System.out.println("Check " + i.getGuard()+"\t"+i.getTrueStatements()+"\t"+i.getFalseStatements());
            }

        }

    }

    public void appliesCommunity (String router, RoutingPolicy rp) {
        if (rp != null) {
            Configuration conf = g.getConfigurations().get(router);
            //System.out.println("CHECK RP " + rp.getStatements()+"\n*****************");
            for ( Statement st : rp.getStatements() ) {
                if ( st instanceof If ) {
                    If i = (If) st;
                    //System.out.println(i.getGuard()+"\t"+i.getTrueStatements()+"\t"+i.getFalseStatements());
                    if (i.getGuard() instanceof Conjunction) {
                        Conjunction cj = (Conjunction) i.getGuard();
                        for (BooleanExpr be : cj.getConjuncts()) {
                            if (be instanceof CallExpr) {
                                CallExpr ce = (CallExpr) be;
                                appliesRP(router, conf.getRoutingPolicies().get(ce.getCalledPolicyName()).getStatements());
                            }
                        }
                    } else if (i.getGuard() instanceof MatchPrefixSet) {
                        MatchPrefixSet m = (MatchPrefixSet) i.getGuard();
                        if (m.getPrefixSet() instanceof NamedPrefixSet) {
                            NamedPrefixSet x = (NamedPrefixSet) m.getPrefixSet();
                            RouteFilterList fl = conf.getRouteFilterLists().get(x.getName());
                            if (fl != null) {
                                for ( RouteFilterLine line : fl.getLines() ) {
                                    //System.out.println("IP. "+ line.getIpWildcard() +"\t"+i.getTrueStatements());
                                    if (line.getIpWildcard().intersects(dstIp)) {
                                        appliesRP(router, i.getTrueStatements());
                                    }        
                                }
                            }
                        }
                    } else if (i.getGuard() instanceof MatchCommunitySet) {
                        MatchCommunitySet m = (MatchCommunitySet) i.getGuard();
                        //System.out.println("@. " + m.getExpr() + "\t" +i.getTrueStatements());
                        // Right now allow it. afterwards need to modify, when it is set
                        CommunitySetExpr ce = m.getExpr();

                        //LineAction la = ce.getAction();
                        for (CommunityVar cv : collectCommunityVars(conf, ce)) {
                            String cvName = ""+cv.getValue().replace("$","").replace("^","");
                            if (isBlockedCommunity(router, cvName)) {
                                _actCommunity.get(router).add(cvName);
                                //System.out.println("Long - " + cvName);
                            }
                            setRouterCommunityCost(i.getTrueStatements(), router, cvName);
                        }
                        appliesRP(router, i.getTrueStatements());
                    }
                }
            }
            //System.out.println("############################");
        }

    }

    public boolean isBlockedCommunity(String router, String cvName) {
        Configuration conf = g.getConfigurations().get(router);
        _routerCommunityLp.put(router, new TreeMap<>());
        for (Entry<String, CommunityList> entry : conf.getCommunityLists().entrySet()) {
            String name = entry.getKey();
            CommunityList cl = entry.getValue();
            for (CommunityListLine cll : cl.getLines()) {
              CommunitySetExpr matchCondition = cll.getMatchCondition();
              LineAction la = cll.getAction();
              
              for (CommunityVar cv : collectCommunityVars(conf, matchCondition)) {
                if(la  == LineAction.DENY && cv.asLong()!=null) {
                  String thisCvName = "" + cv.getValue().replace("$","").replace("^","");
                  //System.out.println(thisCvName + "\tX\t" + cvName);
                  if (thisCvName.equals(cvName))   
                    return true;
                }
              }          
            }
        }
        return false;
    }

    public void setAllCommunity (String router, RoutingPolicy importRP, RoutingPolicy exportRP) {
        Configuration conf = g.getConfigurations().get(router);
        appliesCommunity(router, importRP);
        appliesCommunity(router, exportRP);
    }

    public void buildCommunity() {
        for (Entry<String, List<GraphEdge>> entry : g.getEdgeMap().entrySet()) {
            String router = entry.getKey();
            List<GraphEdge> edges = entry.getValue();
            Configuration conf = g.getConfigurations().get(router);

            for (Protocol proto : _protocols.get(router)) {
                for (GraphEdge e : edges) {
                    if (g.isEdgeUsed(conf, proto, e)) {
                        if (proto.isBgp()) {
                            RoutingPolicy importRP = g.findImportRoutingPolicy(router, proto, e);
                            RoutingPolicy exportRP = g.findExportRoutingPolicy(router, proto, e);
                            setAllCommunity(router, importRP, exportRP);
                        }
                    }
                }
            }
        }

    }

    public boolean ibgpcontains(String srcname, String dstname) {
        boolean hasStatic = dg.isEdge(dg.getVertex(srcname+"-STATIC"), dg.getVertex(dstname+"-STATIC"));
        boolean adjacent = false;
        if (l2map!=null) {
            if (l2map.containsKey(srcname)) {
                adjacent = l2map.get(srcname).containsKey(dstname);
            }
        }
        return (hasStatic || adjacent);
    }

    public void connectRouterSwitch(String routerName, String switchName) {
        for (Node routerProcesses : phyNodeMap.get(routerName)) {
            Node mainSwitch = multigraphNode.get(switchName);
            if (mainSwitch!=null) {
                dg.add(routerProcesses, mainSwitch, returnDefaultEC(), protocol.SWITCH);
                dg.addPhysicalMap(routerProcesses, mainSwitch, countmap);
            }
            for (Node switchVLANs : _switchVLANMap.get(switchName).values()) {
                dg.add(routerProcesses, switchVLANs, returnDefaultEC(), protocol.SWITCH);
                dg.addPhysicalMap(routerProcesses, switchVLANs, countmap);
            }
        }
    }

    public EdgeCost returnDefaultEC() {
        EdgeCost ec = new EdgeCost();

        ec.ospf_cost = 0;
        ec.as_length = 0;
        ec.lp = 0;
        ec.med = 0;           
        ec.rediscost = 0;
        ec.valid = true;    
        return ec;        
    }

    public void connectSwitchRouter(String switchName, String routerName) {
        for (Node routerProcesses : phyNodeMap.get(routerName)) {
            Node mainSwitch = multigraphNode.get(switchName);
            Set<String> allVlans = l2map.get(switchName).get(routerName);

            if ((allVlans.size() == 1 && allVlans.iterator().next().equals("*"))|| allVlans.size()==0) {
                if (mainSwitch!=null) {
                    dg.add(mainSwitch, routerProcesses, returnDefaultEC(), protocol.SWITCH);
                    dg.addPhysicalMap(mainSwitch, routerProcesses, countmap);
                }
            } else {
                for (String aVlan : allVlans) {
                    if (_switchVLANMap.get(switchName).containsKey(aVlan)) {
                        dg.add(_switchVLANMap.get(switchName).get(aVlan), routerProcesses, returnDefaultEC(), protocol.SWITCH);
                        dg.addPhysicalMap(_switchVLANMap.get(switchName).get(aVlan), routerProcesses, countmap);
                    }
                }
            }
        }
    }

    public void connectSwitches(String srcSwitch, String dstSwitch) {
        Set<String> allVlans = l2map.get(srcSwitch).get(dstSwitch);
        Node mainSrc = multigraphNode.get(srcSwitch);
        Node mainDst = multigraphNode.get(dstSwitch);

        if (allVlans.size() == 1 && allVlans.iterator().next().equals("*") || allVlans.size()==0) {
            if (mainSrc != null && mainDst != null){
                dg.add(mainSrc, mainDst, returnDefaultEC(), protocol.SWITCH);
                dg.addPhysicalMap(mainSrc, mainDst, countmap);
            }

            for (String aVlan : allVlans) {
                if (mainSrc != null && _switchVLANMap.get(dstSwitch).containsKey(aVlan)) {
                    dg.add(mainSrc, _switchVLANMap.get(dstSwitch).get(aVlan), returnDefaultEC(), protocol.SWITCH);
                    dg.addPhysicalMap(mainSrc, _switchVLANMap.get(dstSwitch).get(aVlan), countmap);
                }
                if (mainDst != null && _switchVLANMap.get(srcSwitch).containsKey(aVlan)){
                    dg.add(_switchVLANMap.get(srcSwitch).get(aVlan), mainDst, returnDefaultEC(), protocol.SWITCH);
                    dg.addPhysicalMap(_switchVLANMap.get(srcSwitch).get(aVlan), mainDst, countmap);
                }
            }
            
        } else {
            for (String aVlan : allVlans) {
                if (_switchVLANMap.get(srcSwitch).containsKey(aVlan) && _switchVLANMap.get(dstSwitch).containsKey(aVlan)) {
                    dg.add(_switchVLANMap.get(srcSwitch).get(aVlan), _switchVLANMap.get(dstSwitch).get(aVlan), returnDefaultEC(), protocol.SWITCH);
                    dg.addPhysicalMap(_switchVLANMap.get(srcSwitch).get(aVlan), _switchVLANMap.get(dstSwitch).get(aVlan), countmap);
                }
            }
        }

    }

    public void connectVRF(String srcRouter, String dstRouter, String srcname, String dstname, EdgeCost ec, boolean acl, protocol proto) {

        int srcVrfSize = _vrfMap.get(srcRouter).size();
        int dstVrfSize = _vrfMap.get(dstRouter).size();
        if (srcVrfSize == 0 && dstVrfSize == 0) {
            Node src = multigraphNode.get(srcname);
            Node dst = multigraphNode.get(dstname);
            dg.add(src, dst, ec, proto);
            dg.getEdge(src, dst).setIsACL(acl);
            dg.addPhysicalMap(src, dst, countmap);
            if (proto == protocol.IBGP) {
                dg._ibgpPeers.get(src).add(dst);
                dg._ibgpPeers.get(dst).add(src);
            }            
        } else if (srcVrfSize > 0 && dstVrfSize == 0) {
            Node dst = multigraphNode.get(dstname);
            for (String srcVRF : _vrfMap.get(srcRouter)) {
                Node src = multigraphNode.get((srcname+"-"+srcVRF));
                dg.add(src, dst, ec, proto);
                dg.getEdge(src, dst).setIsACL(acl);
                dg.addPhysicalMap(src, dst, countmap);
                if (proto == protocol.IBGP) {
                    dg._ibgpPeers.get(src).add(dst);
                    dg._ibgpPeers.get(dst).add(src);
                }                
            }

        } else if (srcVrfSize == 0 && dstVrfSize > 0) {
            Node src = multigraphNode.get(srcname);
            for (String dstVRF : _vrfMap.get(dstRouter)) {
                Node dst = multigraphNode.get((dstname+"-"+dstVRF));
                dg.add(src, dst, ec, proto);
                dg.getEdge(src, dst).setIsACL(acl);
                dg.addPhysicalMap(src, dst, countmap);
                if (proto == protocol.IBGP) {
                    dg._ibgpPeers.get(src).add(dst);
                    dg._ibgpPeers.get(dst).add(src);
                }                
            }

        } else if (srcVrfSize > 0 && dstVrfSize > 0) {
            for (String vrf : _vrfMap.get(srcRouter)) {
                Node src = multigraphNode.get((srcname+"-"+vrf));
                if (_vrfMap.get(dstRouter).contains(vrf)) {
                    Node dst = multigraphNode.get((dstname+"-"+vrf));
                    if (src == null || dst == null) {
                        /*System.out.println("There is an error in vrf-mapping while creating edges " + (srcname+"-"+vrf)
                         + "\t" + (dstname+"-"+vrf));*/
                    } else {
                        dg.add(src, dst, ec, proto);
                        dg.getEdge(src, dst).setIsACL(acl);
                        dg.addPhysicalMap(src, dst, countmap);
                        if (proto == protocol.IBGP) {
                            dg._ibgpPeers.get(src).add(dst);
                            dg._ibgpPeers.get(dst).add(src);
                        }                
                    }
                }
            }
        }                 
        countmap = countmap + 1;
    }


    public void connectVRF(String srcRouter, String dstRouter, String srcname, String dstname, EdgeCost ec, boolean acl, protocol proto, Interface out, Interface in) {

        int srcVrfSize = _vrfMap.get(srcRouter).size();
        int dstVrfSize = _vrfMap.get(dstRouter).size();
        if (srcVrfSize == 0 && dstVrfSize == 0) {
            Node src = multigraphNode.get(srcname);
            Node dst = multigraphNode.get(dstname);
            dg.add(src, dst, ec, proto);
            aclEdgeMapStart.put(dg.getEdge(src, dst),out);
            aclEdgeMapEnd.put(dg.getEdge(src, dst),in);
            dg.addPhysicalMap(src, dst, countmap);
        } else if (srcVrfSize > 0 && dstVrfSize == 0) {
            Node dst = multigraphNode.get(dstname);
            for (String srcVRF : _vrfMap.get(srcRouter)) {
                Node src = multigraphNode.get((srcname+"-"+srcVRF));
                dg.add(src, dst, ec, proto);
                aclEdgeMapStart.put(dg.getEdge(src, dst),out);
                aclEdgeMapEnd.put(dg.getEdge(src, dst),in);
                dg.addPhysicalMap(src, dst, countmap);
            }

        } else if (srcVrfSize == 0 && dstVrfSize > 0) {
            Node src = multigraphNode.get(srcname);
            for (String dstVRF : _vrfMap.get(dstRouter)) {
                Node dst = multigraphNode.get((dstname+"-"+dstVRF));
                dg.add(src, dst, ec, proto);
                aclEdgeMapStart.put(dg.getEdge(src, dst),out);
                aclEdgeMapEnd.put(dg.getEdge(src, dst),in);
                dg.addPhysicalMap(src, dst, countmap);
            }

        } else if (srcVrfSize > 0 && dstVrfSize > 0) {
            for (String vrf : _vrfMap.get(srcRouter)) {
                Node src = multigraphNode.get((srcname+"-"+vrf));
                if (_vrfMap.get(dstRouter).contains(vrf)) {
                    Node dst = multigraphNode.get((dstname+"-"+vrf));
                    if (src == null || dst == null) {
                        /*System.out.println("There is an error in vrf-mapping while creating edges " + (srcname+"-"+vrf)
                         + "\t" + (dstname+"-"+vrf));*/
                    } else {
                        dg.add(src, dst, ec, proto);
                        aclEdgeMapStart.put(dg.getEdge(src, dst),out);
                        aclEdgeMapEnd.put(dg.getEdge(src, dst),in);
                        dg.addPhysicalMap(src, dst, countmap);
                    }
                }
            }
        }                 
        countmap = countmap + 1;
    }

    public void connectVRF(String srcRouter, String dstRouter, String srcname, String dstname, EdgeCost ec, boolean acl, protocol proto,
    RoutingPolicy importRP, RoutingPolicy exportRP, Interface out, Interface in) {

        int srcVrfSize = _vrfMap.get(srcRouter).size();
        int dstVrfSize = _vrfMap.get(dstRouter).size();
        if (srcVrfSize == 0 && dstVrfSize == 0) {
            Node src = multigraphNode.get(srcname);
            Node dst = multigraphNode.get(dstname);
            dg.add(src, dst, ec, proto);
            Edge thisEdge = dg.getEdge(src, dst);
            aclEdgeMapStart.put(thisEdge, out);
            aclEdgeMapEnd.put(thisEdge, in);
            importMap.put(thisEdge, importRP);
            exportMap.put(thisEdge, exportRP);
            policyConfMap.put(thisEdge, srcRouter);

            dg.addPhysicalMap(src, dst, countmap);
        } else if (srcVrfSize > 0 && dstVrfSize == 0) {
            Node dst = multigraphNode.get(dstname);
            for (String srcVRF : _vrfMap.get(srcRouter)) {
                Node src = multigraphNode.get((srcname+"-"+srcVRF));
                dg.add(src, dst, ec, proto);
                Edge thisEdge = dg.getEdge(src, dst);
                aclEdgeMapStart.put(thisEdge, out);
                aclEdgeMapEnd.put(thisEdge, in);
                importMap.put(thisEdge, importRP);
                exportMap.put(thisEdge, exportRP);
                policyConfMap.put(thisEdge, srcRouter);
                dg.addPhysicalMap(src, dst, countmap);
            }

        } else if (srcVrfSize == 0 && dstVrfSize > 0) {
            Node src = multigraphNode.get(srcname);
            for (String dstVRF : _vrfMap.get(dstRouter)) {
                Node dst = multigraphNode.get((dstname+"-"+dstVRF));
                dg.add(src, dst, ec, proto);
                Edge thisEdge = dg.getEdge(src, dst);
                aclEdgeMapStart.put(thisEdge, out);
                aclEdgeMapEnd.put(thisEdge, in);
                importMap.put(thisEdge, importRP);
                exportMap.put(thisEdge, exportRP);
                policyConfMap.put(thisEdge, srcRouter);
                dg.addPhysicalMap(src, dst, countmap);
            }

        } else if (srcVrfSize > 0 && dstVrfSize > 0) {
            for (String vrf : _vrfMap.get(srcRouter)) {
                Node src = multigraphNode.get((srcname+"-"+vrf));
                if (_vrfMap.get(dstRouter).contains(vrf)) {
                    Node dst = multigraphNode.get((dstname+"-"+vrf));
                    if (src == null || dst == null) {
                        /*System.out.println("There is an error in vrf-mapping while creating edges " + (srcname+"-"+vrf)
                         + "\t" + (dstname+"-"+vrf));*/
                    } else {
                        dg.add(src, dst, ec, proto);
                        Edge thisEdge = dg.getEdge(src, dst);
                        aclEdgeMapStart.put(thisEdge, out);
                        aclEdgeMapEnd.put(thisEdge, in);
                        importMap.put(thisEdge, importRP);
                        exportMap.put(thisEdge, exportRP);
                        policyConfMap.put(thisEdge, srcRouter);
                        dg.addPhysicalMap(src, dst, countmap);
                    }
                }
            }
        }                 
        countmap = countmap + 1;
    }



    public void connectOneSideVRF(String router, String srcname, String dstname, EdgeCost ec, boolean acl, protocol proto) {

        int vrfSize = _vrfMap.get(router).size();
        if (vrfSize == 0) {
            Node src = multigraphNode.get(srcname);
            Node dst = multigraphNode.get(dstname);
            dg.add(src, dst, ec, proto);
            dg.getEdge(src, dst).setIsACL(acl);

        } else if (vrfSize > 0) {
            Node dst = multigraphNode.get(dstname);
            for (String srcVRF : _vrfMap.get(router)) {
                Node src = multigraphNode.get((srcname+"-"+srcVRF));
                dg.add(src, dst, ec, proto);
                dg.getEdge(src, dst).setIsACL(acl);
            }
        }                 
    }


    public boolean hasSameVrf(String router, Node srcprot, Node dstprot) {
        for (String vrf : _vrfMap.get(router)) {
            if (srcprot.getId().contains(vrf) && dstprot.getId().contains(vrf))
                return true;
        }
        return false;
    }

    public void buildEdges() {

    	Node src, dst;
    	String pro, srcnode, dstnode, srcname, dstname;

        if (l2map != null) {
            for (String device : l2map.keySet()) {
                Configuration conf1 = g.getConfigurations().get(device);
                for ( String otherDevice : l2map.get(device).keySet()) {
                    Configuration conf2 = g.getConfigurations().get(otherDevice);

                    if (conf1.getDeviceType()==DeviceType.SWITCH && conf2.getDeviceType()==DeviceType.ROUTER)
                        connectSwitchRouter(device, otherDevice);
                    else if (conf1.getDeviceType()==DeviceType.ROUTER && conf2.getDeviceType()==DeviceType.SWITCH)
                        connectRouterSwitch(device, otherDevice);
                    else if (conf1.getDeviceType()==DeviceType.SWITCH && conf2.getDeviceType()==DeviceType.SWITCH)
                        connectSwitches(device, otherDevice);
                    countmap = countmap + 1;
                }
            }
        }

		// create edges connecting routing processes
    	for (Entry<String, List<GraphEdge>> entry : g.getEdgeMap().entrySet()) {
    		String router = entry.getKey();
    		List<GraphEdge> edges = entry.getValue();
    		Configuration conf = g.getConfigurations().get(router);

    		//System.out.println("VRF " + conf.getVrfs());
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
                            if (g.getIbgpNeighbors().containsKey(e)) {
                                _hasIBGP.add(router);
                                //System.out.println("IBGP " + e.getRouter() + "\t" + e.getPeer());
                                //continue;
                            }
                            RoutingPolicy importRP = g.findImportRoutingPolicy(router, proto, e);
                            RoutingPolicy exportRP = g.findExportRoutingPolicy(router, proto, e);
                            /*
                            if (importRP != null)
                                System.out.println(router + "\tImport\t" + importRP.getStatements());
                            if (exportRP != null)
                                System.out.println(router + "\tExport\t" + exportRP.getStatements());*/
                            /*if(blockFilter(importRP, conf) || blockFilter(exportRP, conf)) {
                                //System.out.print("Filter blocks it");
                                continue;
                            }*/
                            EdgeCost ec = new EdgeCost();
                            srcnode = srcnode + "-BGP";
                            dstnode = dstnode + "-BGP";
                            ec.setAS(1);
              				src = multigraphNode.get(srcnode);
    						dst = multigraphNode.get(dstnode);
                            setBGPCost(importRP, exportRP, ec, conf);
                            //System.out.println("Cost " + ec);
                            boolean acl = blockAcl(e.getEnd(), e.getStart());

                            // ARCHIE avoid acl
                            if (acl == true)
                                continue;
                            if (g.getIbgpNeighbors().containsKey(e)) {
                                //connectVRF(dstname, srcname, dstnode, srcnode, ec, acl, protocol.IBGP, importRP, exportRP, e.getEnd(), e.getStart());
                            }
                            else
                                connectVRF(dstname, srcname, dstnode, srcnode, ec, acl, protocol.BGP, importRP, exportRP, e.getEnd(), e.getStart());

    					} else if (proto.isOspf()) {
    						srcnode = srcnode + "-OSPF";
    						dstnode = dstnode + "-OSPF";
							src = multigraphNode.get(srcnode);
    						dst = multigraphNode.get(dstnode);
    						EdgeCost ec = new EdgeCost();
					        
                            ec.setOSPF(e.getEnd().getOspfCost());
                            boolean acl = blockAcl(e.getStart(), e.getEnd());
                            // ARCHIE avoid acl
                            if (acl == true)
                                continue;
    						connectVRF(srcname, dstname, srcnode, dstnode, ec, acl, protocol.OSPF, e.getEnd(), e.getStart());

    					} else if (proto.isStatic()) {

    						//skipping static routes to unreachable IP
    						if (dstnode == null)
    							continue;    						
							Set<Node> srcRouters = phyNodeMap.get(srcnode);
							Set<Node> dstRouters = phyNodeMap.get(dstnode);

    						srcnode = srcnode + "-STATIC";
    						//dstnode = dstnode + "-STATIC";
							src = multigraphNode.get(srcnode);
    						//dst = multigraphNode.get(dstnode);
    						EdgeCost ec = new EdgeCost();
					        //ec.setAD(50);
                            boolean applies = false;
                            List<StaticRoute> allStatic = g.getStaticRoutes().get(conf.getHostname(), e.getStart().getName()); 
                            /*
                            for (StaticRoute sr : g.getStaticRoutes().get(conf.getHostname(), e.getStart().getName())) {
                                if (dstIp.containsIp(sr.getNextHopIp())) {
                                    applies = true;
                                    int staticWeight = sr.getMetric().intValue();
                                    ec.setWeight(staticWeight);
                                    break;
                                }
                            }*/
                            //if (applies == false)
                            //    continue;

							//dg.add(src, dst, ec, protocol.STAT);
                            //boolean acl = blockAcl(e.getStart(), e.getEnd());
                            //dg.getEdge(src, dst).setIsACL(acl);
                            // ARCHIE avoid acl
                            //if (acl == true)
                            //    continue;
                            
                            for (Node d : dstRouters) {
                                /*if (d.getType() == protocol.STAT)
                                    continue;*/
                                ec.setWeight(10);
                                dg.add(src, d, ec, protocol.STAT);
                                Edge thisEdge = dg.getEdge(src, d);
                                //thisEdge.setIsACL(acl);
                                staticEdgeMap.put(thisEdge, allStatic);
                                aclEdgeMapStart.put(thisEdge , e.getStart());
                                aclEdgeMapEnd.put(thisEdge , e.getEnd());

                            }                            

							/*for (Node s : srcRouters) {
								if (s.getType() != protocol.STAT)
									continue;
								for (Node d : dstRouters) {
									if (d.getType() == protocol.STAT)
										continue;
									dg.add(s, d, ec, protocol.STAT);									
                                    dg.getEdge(s, d).setIsACL(acl);
								}
							}*/
    					}
    				}
    			}
    		}

    	}

        // add IBGP
        /*
        for (Entry<String, List<GraphEdge>> entry : g.getEdgeMap().entrySet()) {
            String router = entry.getKey();
            List<GraphEdge> edges = entry.getValue();
            Configuration conf = g.getConfigurations().get(router);
            for (Protocol proto : _protocols.get(router)) {
                for (GraphEdge e : edges) {
                    if (proto.isBgp() && g.isEdgeUsed(conf, proto, e) && g.getIbgpNeighbors().containsKey(e)) {
                        srcnode = e.getRouter();
                        dstnode = e.getPeer();
                        srcname = e.getRouter();
                        dstname = e.getPeer();

                        if (srcnode == null || dstnode == null)
                            continue;
                        if (ibgpcontains(srcnode, dstnode)) {
                            RoutingPolicy importRP = g.findImportRoutingPolicy(router, proto, e);
                            RoutingPolicy exportRP = g.findExportRoutingPolicy(router, proto, e);
                            //
                            //if(blockFilter(importRP, conf) || blockFilter(exportRP, conf)) {
                            //    continue;
                            //}
                            EdgeCost ec = new EdgeCost();
                            srcnode = srcnode + "-BGP";
                            dstnode = dstnode + "-BGP";
                            ec.setAS(0);
                            src = multigraphNode.get(srcnode);
                            dst = multigraphNode.get(dstnode);
                            setBGPCost(importRP, exportRP, ec, conf);
                            boolean acl = blockAcl(e.getEnd(), e.getStart());
                            //dg.add(dst, src, ec, protocol.IBGP);
                            //dg.getEdge(dst, src).setIsACL(acl);
                            // ARCHIE avoid acl
                            if (acl == true)
                                continue;
                            connectVRF(srcname, dstname, srcnode, dstnode, ec, acl, protocol.IBGP, importRP, exportRP, e.getEnd(), e.getStart());
                        }
                    }
                }
            }
        }*/

        // add edge between ibgp and ospf
        for (String node : phyNodeMap.keySet()) {
            if (!_hasIBGP.contains(node)) {
                //System.out.println(node + " not has" );
                continue;
            }
            Set<Node> allnode = phyNodeMap.get(node);
            for (Node srcprot : allnode) {
                if (srcprot.getType() == protocol.BGP) {
                    for (Node dstprot : allnode) {
                        if (dstprot.getType() == protocol.OSPF) {
                            //System.out.println("Mayadd "+ srcprot +"\t"+ dstprot);
                            //if (hasSameVrf(node, srcprot, dstprot))
                                dg.add(srcprot, dstprot, returnDefaultEC(), protocol.IBGP);
                            //connectVRF(node, node, srcprot.getId(), dstprot.getId(), ec, false, protocol.IBGP);
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
                String protocolName = "";
                //boolean hasSrcIBGP = false;
                protocol srcType = protocol.NONE;
                if (proto.isBgp()) {
                    protocolName = "BGP";
                    srcType = protocol.BGP;
                    /*if (multigraphNode.containsKey(router+"-IBGP")) {
                        hasSrcIBGP = true;
                    }*/
                }
                else if (proto.isOspf()) {
                    protocolName = "OSPF";
                    srcType = protocol.OSPF;
                }
                else if (proto.isStatic()) {
                    protocolName = "STATIC";
                    srcType = protocol.STAT;
                }
                else {
                    //System.out.println(proto);
                }
    			srcnode = router + "-" + protocolName;
    			src = multigraphNode.get(srcnode);
    			RoutingPolicy pol = Graph.findCommonRoutingPolicy(conf, proto);
    			if (pol == null) {
    				continue;
    			}
    			Set<Protocol> ps = g.findRedistributedProtocols(conf, pol, proto);

    			for (Protocol p : ps) {
                    //boolean hasDstIBGP = false;
                    String protocolName2 = "";
                    protocol dstType = protocol.NONE;
                    if (p.isBgp()) {
                        protocolName2 = "BGP";
                        dstType = protocol.BGP;
                        /*if (multigraphNode.containsKey(router+"-IBGP")) {
                            hasDstIBGP = true;
                        }*/
                    }
                    else if (p.isOspf()) {
                        protocolName2 = "OSPF";
                        dstType = protocol.OSPF;
                    }
                    else if (p.isStatic()) {
                        protocolName2 = "STATIC";
                        dstType = protocol.STAT;
                    }
                    else {
                        //System.out.println(p);
                    }

	    			dstnode = router + "-" + protocolName2;

	    			dst = multigraphNode.get(dstnode);
	    			//fix cost
                    if (src == null || dst == null) {
                        //if (hasSrcIBGP == hasDstIBGP) {
                            //System.out.println("Can't add redis between "+ srcnode + " " + dstnode);
                            continue;
                        //}
                    }

					EdgeCost ec = new EdgeCost();
					if (srcType == protocol.BGP && dstType == protocol.OSPF) {
                        ec.setAS(1);
						//dg.add(dst, src, ec, protocol.REDISOB);
                        connectVRF(router, router, dstnode, srcnode, ec, false, protocol.REDISOB);
					} else if (srcType == protocol.OSPF && dstType == protocol.BGP) {
                        ec.setOSPF(1);
						//dg.add(dst, src, ec, protocol.REDISBO);
                        connectVRF(router, router, dstnode, srcnode, ec, false, protocol.REDISBO);
					} else if (srcType == protocol.OSPF && dstType == protocol.STAT) {
                        ec.setOSPF(20);
						//dg.add(src, dst, ec, protocol.REDISSO);
                        connectOneSideVRF(router, srcnode, dstnode, ec, false, protocol.REDISSO);
					} else if (srcType == protocol.BGP && dstType == protocol.STAT) {
                        ec.setAS(1);
                        //dg.add(dst, src, ec, protocol.REDISSB);
                        connectOneSideVRF(router, srcnode, dstnode, ec, false, protocol.REDISSB);
                    } 
    			}
    		}
    	}
    }

    public void setAllCommunities(Node n, String name) {
        //System.out.println(name+"\t"+n);
        if(_actCommunity.containsKey(name))
            n.setBlockCommunity(_actCommunity.get(name));
        if(_addCommunity.containsKey(name))
            n.setAddCommunity(_addCommunity.get(name));
        if(_removeCommunity.containsKey(name))
            n.setRemoveCommunity(_removeCommunity.get(name));
    }

    public void buildNodes() {

    	Node src = null, dst = null;
    	String pro, srcnode, dstnode, srcname, dstname;


        if (l2map != null) {
            for (String device : l2map.keySet()) {
                _switchVLANMap.put(device, new TreeMap<>());
                Configuration conf = g.getConfigurations().get(device);

                if (conf.getDeviceType() == DeviceType.SWITCH) {
                    Node switchNode = new Node(device, protocol.SWITCH);
                    dg.add(switchNode);
                    multigraphNode.put(device, switchNode);

                    for ( String otherDevice : l2map.get(device).keySet()) {
                        for (String vlan : l2map.get(device).get(otherDevice)) {
                            if (vlan!=null && !vlan.equals("*")) {
                                String deviceVLAN = device+"-vlan"+vlan;
                                switchNode = new Node(deviceVLAN, protocol.SWITCH);
                                dg.add(switchNode);
                                multigraphNode.put(deviceVLAN, switchNode);
                                _switchVLANMap.get(device).put(vlan, switchNode);
                            }
                        }
                    }
                }
            }
        }
        //*
        Map<String, Configuration> allConf = g.getConfigurations();
        Node protNode = null;
        String protName = null;
        for (String router : allConf.keySet()) {
            Configuration conf = g.getConfigurations().get(router);
            if (conf.getDeviceType() == DeviceType.SWITCH) {
                continue;
            }
            for (Protocol proto : _protocols.get(router)) {

                if (proto.isBgp()) {
                    protocol thisProtocol = protocol.BGP;
                    protName = router + "-BGP";
                    protNode = new Node(protName, protocol.BGP);
                    dg.add(protNode);
                    multigraphNode.put(protName, protNode);
                    setAllCommunities(protNode, router);
                    phyNodeMap.get(router).add(protNode);
                    dg._ibgpPeers.put(protNode, new HashSet<Node>());

                    for (String vrfName : _vrfMap.get(router)) {
                        String vrfNodeName = protName + "-" + vrfName;
                        Node vrfNode = new Node(vrfNodeName, thisProtocol);
                        dg.add(vrfNode);
                        multigraphNode.put(vrfNodeName, vrfNode);
                        setAllCommunities(vrfNode, router);
                        phyNodeMap.get(router).add(vrfNode);
                        dg._ibgpPeers.put(vrfNode, new HashSet<Node>());
                    }
                } else if (proto.isOspf()) {
                    protocol thisProtocol = protocol.BGP;
                    protName = router + "-OSPF";
                    protNode = new Node(protName, protocol.OSPF);
                    dg.add(protNode);
                    multigraphNode.put(protName, protNode);
                    setAllCommunities(protNode, router);
                    phyNodeMap.get(router).add(protNode);

                    for (String vrfName : _vrfMap.get(router)) {
                        String vrfNodeName = protName + "-" + vrfName;
                        Node vrfNode = new Node(vrfNodeName, thisProtocol);
                        dg.add(vrfNode);
                        multigraphNode.put(vrfNodeName, vrfNode);
                        setAllCommunities(vrfNode, router);
                        phyNodeMap.get(router).add(vrfNode);
                    }
                } else if (proto.isStatic()) {
                    protName = router + "-STATIC";                          
                    protNode = new Node(protName, protocol.STAT);
                    dg.add(protNode);
                    multigraphNode.put(protName, protNode);
                    setAllCommunities(protNode, router);
                    phyNodeMap.get(router).add(protNode);
                }

            }
        }//*/
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
