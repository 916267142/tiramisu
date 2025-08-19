package org.batfish.mulgraph;

import gurobi.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;

public class RKConnected {

  Digraph g;
  GRBEnv env;
  GRBModel model;
  public double runTime = 0;
  public double obj = -1;

  public RKConnected(Digraph graph)
  { 
    g = graph;
    try {
      env = new GRBEnv("kConnected.log");
      model = new GRBModel(env);
    } catch (GRBException e) {
      System.out.println("Error code at Constructor: " + e.getErrorCode() + ". " +
                         e.getMessage());
    }      
  }

  public double returnTime() {
    return runTime;
  }
  
  public double returnObj() {
    return obj;
  }
  
  public void formulate(Node src, Node dst) {
    try {
      g.reverseNeighbors();
      g.setReverseCorrelated();
      g.setCommunity();
      // Create variables
      GRBLinExpr expr, inflow, temp1, corr, sumParentReach, sumGParentReach, tempAdd;
      Map<Node, Map<Node, GRBVar>> flow = new HashMap<>();
      Map<Node, Map<Node, GRBVar>> fail = new HashMap<>();
      List<GRBVar> allFail = new ArrayList<>();
      Map<Node, GRBVar> reachable = new HashMap<>();

      Map<Integer, GRBVar> physicalFailVar = new HashMap<>();

      Map<Node, Map<String, GRBVar>> community = new HashMap<>();

      int constraint = 0;
      for (Node from : g.getVertices()) {
        Map<Node, GRBVar> flowTemp = new HashMap<>();
        Map<Node, GRBVar> failTemp = new HashMap<>();
        for(Edge to : g.getNeighbors(from)) {
          GRBVar failVar;
          GRBVar flowVar = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, ("flow"+from+"-"+to.getDst()) );
          flowTemp.put(to.getDst(), flowVar);

          // failVar set to same physical edge if it exists
          int phyMap = g.getPhysicalMap(from, to.getDst());
          if (phyMap != -1) {
            if (physicalFailVar.containsKey(phyMap)) {
              failVar = physicalFailVar.get(phyMap);
              failTemp.put(to.getDst(), failVar);
              System.out.println("Exists");
              continue;
            }
          }

          failVar = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, ("fail"+from+"-"+to.getDst()) );

          if (phyMap != -1) {
            physicalFailVar.put(phyMap, failVar);
            System.out.println("Added");            
          }

          failTemp.put(to.getDst(), failVar);
          allFail.add(failVar);
        }
        flow.put(from, flowTemp);
        fail.put(from, failTemp);
      }

      for (Node v : g.getVertices()) {
        GRBVar reach = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, ("reach"+v));
        reachable.put(v, reach);
      }

      model.addConstr(reachable.get(src), GRB.EQUAL, 1.0, "reachcons"+constraint);
      constraint = constraint + 1;
      model.addConstr(reachable.get(dst), GRB.EQUAL, 0.0, "reachcons"+constraint);
      constraint = constraint + 1;


      Map<String, Set<Node>> communitySeen = g.getCommunitySeen();
      for ( String comm : communitySeen.keySet() ) {
        for ( Node v : communitySeen.get(comm) ) {
          if (!community.containsKey(v)) {
            community.put(v, new HashMap<>());
          }
          GRBVar canReachCom = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, (v.getId()+""+comm) );
          community.get(v).put(comm, canReachCom);
        }
      }

      // assume the graph which has been given is already pruned and tells me what is the src and dst node

      for (Node v : g.getVertices()) {
        // change it to if-else stmt to avoid use of continue
        if (v == src) {
          continue;
        }

        inflow = new GRBLinExpr();

        for (String comm : v.addedCommunity) {
          model.addConstr(community.get(v).get(comm), GRB.EQUAL, 1.0, "reachcomm"+constraint);
          constraint = constraint + 1;          
        }

        for (String comm : v.removedCommunity) {
          model.addConstr(community.get(v).get(comm), GRB.EQUAL, 0.0, "reachcomm"+constraint);
          constraint = constraint + 1;
        }

        //Map<Node, Map<String, GRBVar>> community = new HashMap<>();
        if (community.containsKey(v)) {
          for (String comm : community.get(v).keySet()) {
            if (v.addedCommunity.contains(comm)||v.removedCommunity.contains(comm)) {
              continue;
            }

            GRBVar reachvcomm = community.get(v).get(comm);
            temp1 = new GRBLinExpr();
            for (Node from : g.inboundNeighbors(v)) {
              if ( communitySeen.get(comm).contains(from) ) {
                model.addConstr(reachvcomm, GRB.GREATER_EQUAL, community.get(from).get(comm), "reachcomm"+constraint);
                constraint = constraint + 1;
                temp1.addTerm(1.0, community.get(from).get(comm));
              }
            }  
            model.addConstr(reachvcomm, GRB.LESS_EQUAL, temp1, "reachcomm"+constraint);
            constraint = constraint + 1;            
            //temp1.addTerm(1.0, reachable.get(from));
          }

        }

        // flow = reach - fail
        // reach = summation(flow)
        for (Node from : g.inboundNeighbors(v)) {
          GRBVar flowcons = flow.get(from).get(v);
          temp1 = new GRBLinExpr();
          temp1.addTerm(1.0, reachable.get(from));
          temp1.addTerm(-1.0, fail.get(from).get(v));

          Edge thisEdge = g.getEdge(from, v);
          if (thisEdge.getType() ==  protocol.DEF && !g.defUsed.contains(thisEdge)) {
            continue;
          }


          if (thisEdge.getType() ==  protocol.IBGP) {
            // check if
            //if (g.isIBGPPair(from, v)) {
              ArrayList<Node> tempList = new ArrayList<Node>(); 

              for (Edge p : g.inboundEdges(from)) {
                if (p.getType() == protocol.OSPF)
                  tempList.add(p.getSrc());
              }

              // can be reached from parent w/o a ospf-edge
              GRBVar nonIBGPParReach = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, ("nonIBGPParReach"+constraint) );
              constraint = constraint + 1;
              sumParentReach = new GRBLinExpr();
              sumParentReach.addConstant(0);

              for (Node parent : g.inboundNeighbors(from)) {
                if (tempList.contains(parent))
                  continue;
                GRBVar parentReach = flow.get(parent).get(from);
                model.addConstr(nonIBGPParReach, GRB.GREATER_EQUAL, parentReach, "nonIBGPParReach"+constraint);
                constraint = constraint + 1;
                sumParentReach.addTerm(1.0, parentReach);
              }
              model.addConstr(nonIBGPParReach, GRB.LESS_EQUAL, sumParentReach, "nonIBGPParReach"+constraint);
              constraint = constraint + 1;

              // can be reached from grandparent to an ospf-edge w/o IBGP edge
              GRBVar nonIBGP_GPReach = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, ("nonIBGP_GPReach"+constraint) );
              constraint = constraint + 1;
              sumGParentReach = new GRBLinExpr();
              sumGParentReach.addConstant(0);
              for (Node v2 : tempList) {
                GRBVar secondFlow = flow.get(v2).get(from);
                for (Node grandParent : g.inboundNeighbors(v2)) {          
                  GRBVar gParentReach = null;
                  constraint = constraint + 1;
                  boolean usedDef = false;
                  if (g.getEdge(grandParent, v2).getType() == protocol.DEF) {
                  // is its great grandparent reachable
                    if (g.inboundNeighbors(grandParent).size() <= 0) {
                      System.out.println("Incorrect graph DEF has an non BGP parent");
                    }
                    Node greatGP = g.inboundNeighbors(grandParent).get(0);
                    gParentReach = flow.get(greatGP).get(grandParent);
                    
                  } else if (g.getEdge(grandParent, v2).getType() == protocol.BGP) {
                    gParentReach = flow.get(grandParent).get(v2);
                  } else {
                    continue;
                  }

                  //tempV1 is 1 if both gParentReach and secondFlow is 1
                  GRBVar tempV1 = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, ("tempV1"+constraint) );
                  model.addConstr(tempV1, GRB.LESS_EQUAL, secondFlow, "nonIBGP_GPReach"+constraint);
                  constraint = constraint + 1;

                  model.addConstr(tempV1, GRB.LESS_EQUAL, gParentReach, "nonIBGP_GPReach"+constraint);
                  constraint = constraint + 1;

                  tempAdd = new GRBLinExpr();
                  tempAdd.addTerm(1.0, secondFlow);
                  tempAdd.addTerm(1.0, gParentReach);
                  tempAdd.addConstant(-1.0);
                  model.addConstr(tempV1, GRB.GREATER_EQUAL, tempAdd, "nonIBGP_GPReach"+constraint);
                  constraint = constraint + 1;

                  model.addConstr(nonIBGP_GPReach, GRB.GREATER_EQUAL, tempV1, "nonIBGP_GPReach"+constraint);
                  constraint = constraint + 1;
                  sumGParentReach.addTerm(1.0, tempV1);
                }
              }
              model.addConstr(nonIBGP_GPReach, GRB.LESS_EQUAL, sumGParentReach, "nonIBGP_GPReach"+constraint);
              constraint = constraint + 1;

              GRBVar canReach = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, ("canReach"+constraint) );
              constraint = constraint + 1;
              model.addConstr(canReach, GRB.GREATER_EQUAL, nonIBGPParReach, "canReach"+constraint);
              constraint = constraint + 1;
              model.addConstr(canReach, GRB.GREATER_EQUAL, nonIBGP_GPReach, "canReach"+constraint);
              constraint = constraint + 1;

              tempAdd = new GRBLinExpr();
              tempAdd.addTerm(1.0, nonIBGPParReach);
              tempAdd.addTerm(1.0, nonIBGP_GPReach);
              model.addConstr(canReach, GRB.LESS_EQUAL, tempAdd, "canReach"+constraint);
              constraint = constraint + 1;

              GRBVar onlyIBGP = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, ("onlyIBGP"+constraint) );
              constraint = constraint + 1;
              tempAdd = new GRBLinExpr();
              tempAdd.addConstant(1.0);
              tempAdd.addTerm(-1.0, canReach);
              model.addConstr(onlyIBGP, GRB.EQUAL, tempAdd, "onlyIBGP"+constraint);
              constraint = constraint + 1;

              temp1.addTerm(-1.0, onlyIBGP);
            /*}/* else {
              continue;
            }*/
          }

          GRBVar blockCom = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, ("blockcommunity"+constraint) );
          constraint = constraint + 1;

          tempAdd = new GRBLinExpr();

          for (String comm : v.blockedCommunity) {
            if (communitySeen.get(comm).contains(from)) {
              model.addConstr(blockCom, GRB.GREATER_EQUAL, community.get(from).get(comm), "reachcomm"+constraint);
              constraint = constraint + 1;
              tempAdd.addTerm(1.0, community.get(from).get(comm));
            }
          }
          model.addConstr(blockCom, GRB.LESS_EQUAL, tempAdd, "reachcomm"+constraint);
          constraint = constraint + 1;
          temp1.addTerm(-1.0, blockCom);


          
          GRBVar tempVar = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, ("tempVar"+constraint) );
          constraint = constraint + 1;
          model.addConstr(tempVar, GRB.GREATER_EQUAL, temp1, "flowcons"+constraint);
          constraint = constraint + 1;
          model.addConstr(flowcons, GRB.GREATER_EQUAL, tempVar, "flowcons"+constraint);
          constraint = constraint + 1;


          inflow.addTerm(1.0, flowcons);
          model.addConstr(reachable.get(v), GRB.GREATER_EQUAL, flowcons, "reach-or-cons"+constraint);
          constraint = constraint + 1;
        }


        model.addConstr(reachable.get(v), GRB.LESS_EQUAL, inflow, "flowcons"+constraint);
        constraint = constraint + 1;

      }
      

      // Set objective:
      expr = new GRBLinExpr();
      for (GRBVar v : allFail) {
        expr.addTerm(1.0, v);
      }
      model.setObjective(expr, GRB.MINIMIZE);

    } catch (GRBException e) {
      System.out.println("Error code at formulation: " + e.getErrorCode() + ". " +
                         e.getMessage());
    }
  }

  public double run(){
    double time = 0;
     try {
        model.set(GRB.IntParam.OutputFlag, 0);
        //model.write("out.rlp");
         // Optimize model
        model.optimize();

        if(model.get(GRB.IntAttr.Status) == GRB.Status.INFEASIBLE){
            //System.out.println("There is no optimal solution "+ model.get(GRB.IntAttr.Status));
            //model.computeIIS();
            //model.write("model.ilp");

        } else {                          

          //System.out.println("Obj: " + model.get(GRB.DoubleAttr.ObjVal));
          
          obj = model.get(GRB.DoubleAttr.ObjVal);
          /*time = model.get(GRB.DoubleAttr.Runtime) * 1000;
          runTime = time;*/
          //System.out.println("ILP Time: " + time + " ms");

          // Dispose of model and environment
          //model.write("out.sol");
        }

        //System.out.println("Number of variables: " + model.get(GRB.IntAttr.NumVars));
        //System.out.println("Number of constraints: " + model.get(GRB.IntAttr.NumConstrs));
        //
        model.dispose();
        env.dispose();
    } catch (GRBException e) {
      System.out.println("Error code at optimization: " + e.getErrorCode() + ". " +
                         e.getMessage());
    }
    return time;
  }
}