package org.batfish.mulgraph;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class VerificationTpg implements Runnable {

	Tpg tpg;
	double time = 0;
	TpgNode src = null;
	TpgNode dst = null;
	policyName policy = policyName.NONE;
	TpgNode waypoint = null;

	public double returnTime() {
		return time;
	}

	public VerificationTpg(Tpg tpg_in) {	
		tpg = tpg_in;
	}

	public VerificationTpg(Tpg tpg_in, policyName pol) {	
		tpg = tpg_in;
		policy = pol;
	}

	public void setSrcDstTC(TpgNode srcNode, TpgNode dstNode) {
		src = srcNode;
		dst = dstNode;
	}

	public void removeNode(TpgNode v) {
		tpg.getNeighborMap().put(v, new ArrayList<TpgEdge>());
	}

	public boolean alwaysWaypoint() {
		//removeNode(waypoint);
		if (alwaysBlocked() == true) {
			System.out.println( "already unreachable.");
			return true;
		}
		//change this to waypointnode
		List<TpgNode> way = tpg.outboundNeighbors(src);
		if (way.size() > 0)
		removeNode(way.get(0));
		if (alwaysBlocked()) {
			System.out.println( "Always waypoint: True");
			return true;		
		} else {
			System.out.println( "Always waypoint: False");
			return false;				
		}
	}

	/*
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

	//public boolean alwaysChainWaypoint(Node src, Node dst, List<Node> chain) {
	//	removeNode(waypoint);
	//	return alwaysBlocked(src, dst);		
	//}
	*/
    public void run() {
        if (policy == policyName.BLOCK) {
        	//System.out.println(src + "\t" + dst + "\t" + alwaysBlocked());
        	alwaysBlocked();
        } else if (policy == policyName.FAIL) {
			System.out.print("test failure");
        	fail();
        } else if (policy == policyName.EQUAL) {
        	equalLength();
        } else if (policy == policyName.BOUND) {
        	boundLength();
        } else if (policy == policyName.BF) {
        	getPath();
        }  else if (policy == policyName.TPVP) {
        	getTPVPPath();
        } else if (policy == policyName.PREF) {
        	prefPath();
        }  else if (policy == policyName.WAY) {
        	alwaysWaypoint();
        } else if (policy == policyName.NONE) {
        	/*
        	UnreachableTaint unreach = new UnreachableTaint(g);
        	//unreach.isUnreachable(src, dst);
        	//System.out.println(src + "\t" + dst + "\t" + unreach.isUnreachable(src, dst));
        	unreach.isUnreachable(src, dst);
        	*/
        	failsimple();
		}
    }


	public double failsimple() {
        removeUnreach(src, dst);
		ilpMinCut pl = new ilpMinCut(tpg);
		pl.formulate(src, dst);
        pl.run();
		System.out.println("Failures to disconnect the graph " + pl.returnObj());
        //TPVP_BF tpvp = new TPVP_BF(tpg);
        //tpvp.shortestPath(src, dst);
        //tpvp.shortestPath(src, dst);
        
        TPVP_BF tpvp = new TPVP_BF(tpg);
        //System.out.println(tpvp.shortestPath(src, dst));
        tpvp.shortestPath(src, dst);

        tpvp.shortestPath(src, dst);
        return 0;
        //TODO: Will update
	}



	public double fail() {
        removeUnreach(src, dst);
		ilpMinCut pl = new ilpMinCut(tpg);
		pl.formulate(src, dst);
        pl.run();
		System.out.println("Failures to disconnect the graph " + pl.returnObj());
        //TPVP_BF tpvp = new TPVP_BF(tpg);
        //tpvp.shortestPath(src, dst);
        //tpvp.shortestPath(src, dst);
        
        TPVP_BF tpvp = new TPVP_BF(tpg);
        //System.out.println(tpvp.shortestPath(src, dst));
        tpvp.shortestPath(src, dst);

		int numThreads = Runtime.getRuntime().availableProcessors();
		ExecutorService pool = Executors.newFixedThreadPool(numThreads);
        int count = 2;
        HashSet<TpgEdge> edgeSet = tpvp.getAllEdges();
        //System.out.println(edgeSet);
        for(TpgEdge e : edgeSet) {
        	Runnable ntpvp = new TPVP_BF(tpg, e, src, dst);
        	pool.execute(ntpvp);
        }
		pool.shutdown();

		try {
		  pool.awaitTermination(5000, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
		  System.out.println("Path preference interrupted. EXIT");
		  System.exit(0);
		}



        return 0;
        //TODO: Will update
	}

	public void removeUnreach(TpgNode src, TpgNode dst) {
        HashMap<TpgNode, Boolean> visited = new HashMap<>(); 

        for (TpgNode v : tpg.getVertices()) {
            visited.put(v, false);
        }
        isReachableAny(src, dst, visited);
        /*
        for (TpgNode v : tpg.getVertices()) {
            if (visited.get(v) == false) {
            	removeNode(v);
            }
        }
        */

		// let's use Iterator to remove a key from HashMap while iterating
		Iterator<TpgNode> iterator = tpg.getNeighborMap().keySet().iterator();
		//System.out.println( tpg.getNumberOfEdges() + "\t" + tpg.getVertices().size());
		while(iterator.hasNext()){
		  TpgNode v = iterator.next();
		  if(visited.get(v) == false && v!=dst){
		    iterator.remove();
		  }
		}
		//System.out.println( tpg.getNumberOfEdges() + "\t" + tpg.getVertices().size());


	}

    private Boolean isReachableAny(TpgNode u, TpgNode d, 
        HashMap<TpgNode, Boolean> visited) { 
          
        // Mark the current TpgNode 
        visited.put(u, true);
        //System.out.println(u + " - " +d);

        // Recur for all the vertices 
        // adjacent to current TpgNode 
        for(TpgEdge e: tpg.getNeighbors(u)) {
            protocol current = e.getType(); 
            TpgNode i = e.getDst();

            if (visited.get(i) == false) {
                isReachableAny(i, d, visited);
            } 
        } 
        return false;          
    }

	public boolean getPath() {		
        /*
        TPVP_BF tpvp = new TPVP_BF(tpg);
        //System.out.println(tpvp.shortestPath(src, dst));
        tpvp.shortestPath(src, dst);
        return true;*/

        removeUnreach(src, dst);
        TPVP_BF tpvp = new TPVP_BF(tpg);
        //TPVP tpvp = new TPVP(tpg);
        tpvp.shortestPath(src, dst);
        //tpvp.shortestPath(src, dst);
        return true;

	}

	public boolean getTPVPPath() {		
        removeUnreach(src, dst);
        TPVP_BF tpvp = new TPVP_BF(tpg);
        //System.out.println(tpvp.shortestPath(src, dst));
        tpvp.shortestPath(src, dst);
        tpvp.shortestPath(src, dst);
        //System.out.println(tpvp.getActualPath());
        //System.out.println(tpvp.getAllEdges());
        return true;
	}


	public boolean prefPath() {
        removeUnreach(src, dst);
        //TPVP tpvp = new TPVP(tpg);
        //tpvp.shortestPath(src, dst);
        TPVP_BF tpvp = new TPVP_BF(tpg);
        //System.out.println(tpvp.shortestPath(src, dst));
        tpvp.shortestPath(src, dst);

		int numThreads = Runtime.getRuntime().availableProcessors();
		ExecutorService pool = Executors.newFixedThreadPool(numThreads);
        int count = 2;
        HashSet<TpgEdge> edgeSet = tpvp.getAllEdges();
        //System.out.println(edgeSet);
        for(TpgEdge e : edgeSet) {
        	Runnable ntpvp = new TPVP_BF(tpg, e, src, dst);
        	pool.execute(ntpvp);
        }
		pool.shutdown();

		try {
		  pool.awaitTermination(5000, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
		  System.out.println("Path preference interrupted. EXIT");
		  System.exit(0);
		}


        /*
        TPVP tpvp = new TPVP(tpg);
        tpvp.shortestPath(src, dst);
        int count = 2;
        HashSet<TpgEdge> edgeSet = tpvp.getAllEdges();
        //System.out.println(edgeSet);
        for(TpgEdge e : edgeSet) {
        	tpvp.addFailEdge(e);
        	tpvp.initializeGraph();
        	//System.out.println(tpvp.shortestPath(src, dst));
        	tpvp.shortestPath(src, dst);
        	tpvp.removeFailEdge(e);
        }

        */
        return true;
	}


	public boolean boundLength() {
		
        removeUnreach(src, dst);
		ilpPathLength pl = new ilpPathLength(tpg);
		pl.formulate(src, dst, 1);
		double time1 = pl.run();
		double obj = pl.returnObj();
		//System.out.println( (time1 + time2)+ " ms" );
		System.out.println( "Max Length\t" + obj );
		return (obj!=-1 && (obj == 5));
	}

	public boolean equalLength() {
        removeUnreach(src, dst);		
		ilpPathLength pl = new ilpPathLength (tpg);
		pl.formulate(src, dst, 1);
		double time1 = pl.run();
		double min = pl.returnObj();
		pl.reset();
		pl.formulate(src, dst, 0);
		double time2 = pl.run();
		double max = pl.returnObj();
		//System.out.println( (time1 + time2)+ " ms" );
		System.out.println( "min length: " + min + "\tmax length: " + max );
		return (max!=-1 && (max == min));
		
	}

	public boolean alwaysBlocked() {
		//System.out.println("Block code");
		if (src==null || dst==null)
			return false;
		UnreachableTpg unreach = new UnreachableTpg(tpg);
		//boolean result = unreach.isUnreachableTpg(src, dst);
		boolean result = unreach.isUnreachableTpg(src, dst);
		System.out.println("Always Blocked: " + result);
		return result;
	}

	/*
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
		}*/
                                                                                                 
}
