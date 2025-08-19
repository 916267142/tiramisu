package org.batfish.mulgraph;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

public class Verification implements Runnable {

	Digraph g;
	double time = 0;
	Node src = null;
	Node dst = null;
	policyName policy = policyName.NONE;
	Node waypoint = null;

	public double returnTime() {
		return time;
	}

	public Verification(Digraph graph) {	
		g = graph;
	}

	public Verification(Digraph graph, policyName pol) {	
		g = graph;
		policy = pol;
	}

	public void setSrcDstTC(Node srcNode, Node dstNode) {
		src = srcNode;
		dst = dstNode;
	}

	public void removeNode(Node v) {
		g.getNeighborMap().put(v, new ArrayList<Edge>());
	}

	public boolean alwaysWaypoint() {
		//removeNode(waypoint);
		if (alwaysBlocked(src, dst) == true)
			return true;
		//change this to waypointnode
		List<Node> way = g.outboundNeighbors(src);
		if (way.size() > 0)
		removeNode(way.get(0));
		return alwaysBlocked(src, dst);		
	}

	public double fail(Node srcNode, Node dstNode) {
		src = srcNode;
		dst = dstNode;
		return fail();
	}

	public double fail() {
		RKConnected rk = new RKConnected(g);
		rk.formulate(dst, src);
		time = rk.run();
		//System.out.println("time " + time + "\t obj: " + rk.returnObj());
		return rk.returnObj();
	}

	/*
	public boolean alwaysChainWaypoint(Node src, Node dst, List<Node> chain) {
		removeNode(waypoint);
		return alwaysBlocked(src, dst);		
	}
	*/

    public void run() {
        if (policy == policyName.BLOCK) {
        	//System.out.println(src + "\t" + dst + "\t" + alwaysBlocked());
        	alwaysBlocked();
        } else if (policy == policyName.FAIL) {
        	fail();
        } else if (policy == policyName.EQUAL) {
        	equalLength();
        } else if (policy == policyName.BOUND) {
        	boundLength();
        } else if (policy == policyName.PREF) {
        	prefPath();
        } else if (policy == policyName.WAY) {
        	alwaysWaypoint();
        } else if (policy == policyName.BF) {
        	getPath();
        } else if (policy == policyName.NONE) {
        	UnreachableTaint unreach = new UnreachableTaint(g);
        	//unreach.isUnreachable(src, dst);
        	//System.out.println(src + "\t" + dst + "\t" + unreach.isUnreachable(src, dst));
        	unreach.isUnreachable(src, dst);
		} 
    }

	public boolean getPath() {		
        BF bf = new BF(g);
        System.out.println(bf.shortestPath(src, dst));
        return true;
	}


	public boolean prefPath() {
		
        /*
        BF bf = new BF(g);
        bf.shortestPath(src, dst);
        bf.initializeGraph();
        bf.shortestPath(src, dst);
        bf.initializeGraph();
        bf.shortestPath(src, dst);
        bf.initializeGraph();
        bf.shortestPath(src, dst);
        bf.initializeGraph();
        bf.shortestPath(src, dst);
        bf.initializeGraph();
        bf.shortestPath(src, dst);
        bf.initializeGraph();*/
        //System.out.println("start");
        //System.out.println(bf.shortestPath(src, dst));
        //bf.initializeGraph();
        RYen yen = new RYen(g);
        //System.out.println(yen.ksp(src, dst, 4));
        yen.ksp(src, dst, 4);
        return true;
	}


	public boolean boundLength() {
		
		RPathLength pl = new RPathLength(g);
		pl.reverse();
		pl.formulate(dst, src, 1);
		double time1 = pl.run();
		double obj = pl.returnObj();
		//System.out.println( (time1 + time2)+ " ms" );
		//System.out.println( min + "\t" + max );
		return (obj!=-1 && (obj == 5));
		
	}

	public boolean equalLength() {
		
		RPathLength pl = new RPathLength(g);
		pl.reverse();
		pl.formulate(dst, src, 1);
		double time1 = pl.run();
		double min = pl.returnObj();
		pl.reset();
		pl.formulate(dst, src, 0);
		double time2 = pl.run();
		double max = pl.returnObj();
		//System.out.println( (time1 + time2)+ " ms" );
		//System.out.println( min + "\t" + max );
		return (max!=-1 && (max == min));
		
	}

	public boolean alwaysBlocked(Node srcName, Node dstName) {
		src = srcName;
		dst = dstName;
		return alwaysBlocked();
	}


	public boolean alwaysBlocked() {
		if (src==null || dst==null)
			return false;
		Unreachable unreach = new Unreachable(g);
		if ((g.communityBlocked.size()==0)|| g.communityAdded.isEmpty()){
			return unreach.isUnreachable(src, dst);
		}

		if (unreach.isUnreachable(src, dst)) {
			return true;
		}
		// This means nodes are reachable. Now we should check with communities

		//System.out.println("Checking community");
		Map<Node, List<Edge>> copyMap = g.returnCopyNeighborMap();
		Map<String, Boolean> isUnreach = new HashMap<>();
		boolean mayReach = false;

		for (String comm : g.communityBlocked) {

			//System.out.println("Checking with blocking node removed" + g.communityBlockNodes.get(comm));
			for (Node blockNode : g.communityBlockNodes.get(comm)) {
				removeNode(blockNode);
			}

			if (!unreach.isUnreachable(src, dst)) {
				g.setNeighborMap(copyMap);
				continue;
			}
			// this means all routes from src to dst goes through blocking-node on community
			g.setNeighborMap(copyMap);


			if (g.communityAdded.containsKey(comm)) {
				//System.out.println("Checking with added node removed" + g.communityAdded.get(comm));
				for (Node addedNode : g.communityAdded.get(comm)) {
					removeNode(addedNode);
				}
			} else {
				// no one is adding this community, so ignore
				//isUnreach.put(comm, null);
				g.setNeighborMap(copyMap);
				continue;
			}

			if (!unreach.isUnreachable(src, dst)) {
				g.setNeighborMap(copyMap);
				continue;
			}
			// this means all routes from src to dst goes through both nodes adding and blocking on community

			//g.setNeighborMap(copyMap);

			mayReach = false;
			for (Node blockNode : g.communityBlockNodes.get(comm)) {
				if (!unreach.isUnreachable(blockNode, dst)) {
					mayReach = true;;
				}	
			}

			g.setNeighborMap(copyMap);
			if (mayReach == true) {
				continue;
			}

			// after removing community-added nodes, block nodes can't reach destination

			// this means all routes from community-blocking-node to dst will go through community-adding-node
			/*
			if (g.communityRemoveNodes.containsKey(comm)) {	
				System.out.println("Checking with removing node removed");
				for (Node removeNode : g.communityRemoveNodes.get(comm)) {
					removeNode(removeNode);
				}
			}

			//boolean reach = true;
			for (Node addedNode : g.communityAdded.get(comm)) {
				for (Node blockNode : g.communityBlockNodes.get(comm)) {
					if (!unreach.isUnreachable(addedNode, blockNode)) { //nodes can reach
						return false;
					}						
				}
			}*/

			mayReach = false;
			for (Node blockNode : g.communityBlockNodes.get(comm)) {
				if (g.communityRemoveNodes.containsKey(comm)) {	
					for (Node removeNode : g.communityRemoveNodes.get(comm)) {
						if (!unreach.isUnreachable(blockNode, removeNode)) {
							for (Node addedNode : g.communityAdded.get(comm)) {
								if (!unreach.isUnreachable(removeNode, addedNode)) {
									mayReach = true;
									break;
								}
							}
						}
					}
				}
			}
			g.setNeighborMap(copyMap);
			if (mayReach == true) {
				continue;
			}
			return true;
		}
		return false;
	}
                                                                                                 
}