package org.batfish.mulgraph;

import java.util.Set;
import java.util.HashSet;

public class RagNode {

	String id;
    protocol type;
    public boolean taint = false;

    public RagNode(String x, protocol y){
        id = x;
        type = y;
    }

    public String getId() {
        return id;
    }

    public protocol getProtocol() {
        return type;
    }

    public boolean getTaint() {
        return taint;
    }

    public RagNode copy() {
        return new RagNode(id, type);
    }

    @Override
    public String toString() {
        return "Node=" + id + " Protocol=" + type;
    }
}