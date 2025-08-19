package org.batfish.mulgraph;

import java.util.Set;
import java.util.HashSet;

public class Node {

	String id;
	protocol type;
    public Set<String> addedCommunity;
    public Set<String> removedCommunity;
    public Set<String> blockedCommunity;
    public boolean taint = false;

    public Node(String x, protocol y){
        id = x; type = y;
        addedCommunity = new HashSet<>();
        removedCommunity = new HashSet<>();
        blockedCommunity = new HashSet<>();
    }

    public String getId() {
    	return id;
    }

    public protocol getType() {
    	return type;
    }

    public Node copy() {
        return new Node(id, type);
    }

    @Override
    public String toString() {
        return "Node=" + id;// + " Add " + addedCommunity + " Remove " + removedCommunity + " Block " + blockedCommunity;
    }

    public void addCommunity(String community) {
        addedCommunity.add(community);
    }

    public void removeCommunity(String community) {
        removedCommunity.add(community);
    }

    public void blockCommunity(String community) {
        blockedCommunity.add(community);
    }

    public void setBlockCommunity(Set<String> community) {
        for (String comm : community) {
            blockedCommunity.add(comm);
        }
    }

    public void setAddCommunity(Set<String> community) {
        for (String comm : community) {
            addedCommunity.add(comm);
        }
    }

    public void setRemoveCommunity(Set<String> community) {
        for (String comm : community) {
            removedCommunity.add(comm);
        }
    }

}