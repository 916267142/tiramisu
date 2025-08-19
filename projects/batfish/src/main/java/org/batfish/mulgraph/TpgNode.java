package org.batfish.mulgraph;

import java.util.Set;
import java.util.HashSet;

public class TpgNode {

	String id;
    protocol type;
    String device;

    public TpgNode(String x, protocol y, String dev){
        id = x;
        type = y;
        device = dev;
    }

    public String getId() {
        return id;
    }

    public String getDevice() {
        return device;
    }

    public protocol getProtocol() {
        return type;
    }

    public TpgNode copy() {
        return new TpgNode(id, type, device);
    }

    @Override
    public String toString() {
        return "Node=" + id;// + " Protocol=" + type;
    }
}