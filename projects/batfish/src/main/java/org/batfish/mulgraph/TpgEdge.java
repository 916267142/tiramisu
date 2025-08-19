package org.batfish.mulgraph;

import java.util.HashMap;
import java.util.Map;
import java.util.Collections;

public class TpgEdge {
    TpgNode src;
    TpgNode vertex;
    EdgeCost cost;
    protocol type;
    boolean hasACL = false;
    boolean canRemove = false;
    boolean failed = false;


    static final Map<protocol, Integer> protocol_map = createMap();

    static Map<protocol, Integer> createMap() {
        Map<protocol, Integer> result = new HashMap<protocol, Integer>();
        // STAT, OSPF, BGP, REDISOB, REDISBO, IBGP, DEF, NONE;
        result.put(protocol.STAT, 1);
        result.put(protocol.OSPF, 100);
        result.put(protocol.BGP, 20);
        result.put(protocol.REDISOB, 20);
        result.put(protocol.REDISBO, 100);
        result.put(protocol.IBGP, 200);
        result.put(protocol.DEF, 100);
        result.put(protocol.NONE, 100000);
        result.put(protocol.INTF, 10000);
        result.put(protocol.RIB, 1000);
        result.put(protocol.REDISSB, 20);
        result.put(protocol.REDISSO, 100);
        result.put(protocol.SWITCH, 10000);
        result.put(protocol.SRC, 0);        
        result.put(protocol.DST, 0);        

        return Collections.unmodifiableMap(result);
    }

    public TpgEdge(TpgNode s, TpgNode v, EdgeCost c){
        src = s; vertex = v; cost = c; type = protocol.NONE;
        cost.AD = protocol_map.get(type);
    }

    public TpgEdge(TpgNode s, TpgNode v, EdgeCost c, protocol p){
        src = s; vertex = v; cost = c; type = p;
        cost.AD = protocol_map.get(type);
    }


    public TpgEdge(TpgNode s, TpgNode v, EdgeCost c, protocol p, boolean acl, boolean removable){
        src = s; vertex = v; cost = c; type = p; hasACL = acl; canRemove = removable;
        cost.AD = protocol_map.get(type);
    }

    public TpgNode getDst() {
        return vertex;
    }

    public TpgNode getSrc() {
        return src;
    }

    public EdgeCost getCost() {
        return cost;
    }

    public void setCost(EdgeCost ec) {
        cost = ec;
    }

    public protocol getType() {
        return type;
    }

    public boolean isACL() {
        return hasACL;
    }

    public void setACL() {
        hasACL = true;
    }

    public void setRemovable() {
        canRemove = true;
    }

    @Override
    public String toString() {
        return "Edge [src=" + src + ", dst=" + vertex + ", cost=" + cost/* + " type: " + type */ + "]";
    }

    public TpgEdge copy() {
        return new TpgEdge(src, vertex, cost.copy(), type, hasACL, canRemove);
    }

}

