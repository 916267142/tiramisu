package org.batfish.mulgraph;

import java.util.HashMap;
import java.util.Map;
import java.util.Collections;

public class RagEdge {
    RagNode src;
    RagNode vertex;
    protocol type;

    public RagEdge(RagNode s, RagNode v){
        src = s; vertex = v; type = protocol.NONE;
    }

    public RagEdge(RagNode s, RagNode v, protocol p){
        src = s; vertex = v; type = p;
    }

    public RagNode getDst() {
        return vertex;
    }

    public RagNode getSrc() {
        return src;
    }

    public protocol getType() {
        return type;
    }

    @Override
    public String toString() {
        return "Edge [src=" + src + ", dst=" + vertex + " type: " + type  + "]";
    }

    public RagEdge copy() {
        return new RagEdge(src, vertex, type);
    }

}

