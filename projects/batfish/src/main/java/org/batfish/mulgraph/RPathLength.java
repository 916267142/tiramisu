package org.batfish.mulgraph;

import gurobi.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;

public class RPathLength {

  Digraph g;
  GRBEnv env;
  GRBModel model;
  double obj= -1;

  public RPathLength(Digraph graph)
  { 
    g = graph;
    try {
      env = new GRBEnv("RPathLength.log");
      model = new GRBModel(env);
    } catch (GRBException e) {
      System.out.println("Error code at Constructor: " + e.getErrorCode() + ". " +
                         e.getMessage());
    }      
  }

  public double returnObj() {
    return obj;
  }

  public void reverse() {
      g.reverseNeighbors();
      g.setReverseCorrelated();
      g.setCommunity();
  }

  public void reset() {
    try {
      env = new GRBEnv("RPathLength.log");
      model = new GRBModel(env);
    } catch (GRBException e) {
      System.out.println("Error code at Constructor: " + e.getErrorCode() + ". " +
                         e.getMessage());
    }      
  }

  
  public void formulate(Node src, Node dst, int length) {
    try {
      
      // Create variables
      GRBLinExpr expr, inflow, temp1, corr, sumParentReach, sumGParentReach, tempAdd, outflow;
      Map<Node, Map<Node, GRBVar>> flow = new HashMap<>();

      List<GRBVar> allFlows = new ArrayList<>();
      Map<Node, Map<String, GRBVar>> community = new HashMap<>();

      int constraint = 0;
      for (Node from : g.getVertices()) {
        Map<Node, GRBVar> flowTemp = new HashMap<>();
        for(Edge to : g.getNeighbors(from)) {
          GRBVar flowVar = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, ("flow"+from+"-"+to.getDst()) );
          flowTemp.put(to.getDst(), flowVar);
          allFlows.add(flowVar);
        }
        flow.put(from, flowTemp);
      }


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

      for (Node v : g.getVertices()) {
        outflow = new GRBLinExpr(); 
        inflow = new GRBLinExpr();
        inflow.addConstant(0.0);
        outflow.addConstant(0.0);
        for (Node from : g.inboundNeighbors(v)) {
          GRBVar flowcons = flow.get(from).get(v);
          inflow.addTerm(1.0, flowcons);
        }
        for (Node to : g.outboundNeighbors(v)) {
          GRBVar flowcons = flow.get(v).get(to);
          outflow.addTerm(1.0, flowcons);
        }


        if (v == src) {
          model.addConstr(outflow, GRB.EQUAL, 1.0, "flowcons"+constraint);
          constraint = constraint + 1;                    
        } else if (v == dst) {
          model.addConstr(inflow, GRB.EQUAL, 1.0, "flowcons"+constraint);
          constraint = constraint + 1;                    
        } else {
          model.addConstr(outflow, GRB.EQUAL, inflow, "flowcons"+constraint);
          constraint = constraint + 1;          
        }

      }

      // assume the graph which has been given is already pruned and tells me what is the src and dst node

      for (Node v : g.getVertices()) {
        // change it to if-else stmt to avoid use of continue

        inflow = new GRBLinExpr();

        for (String comm : v.addedCommunity) {
          model.addConstr(community.get(v).get(comm), GRB.EQUAL, 1.0, "reachcomm"+constraint);
          constraint = constraint + 1;          
        }

        for (String comm : v.removedCommunity) {
          model.addConstr(community.get(v).get(comm), GRB.EQUAL, 0.0, "reachcomm"+constraint);
          constraint = constraint + 1;          
        }

        if (v == src) {
          continue;
        }

        //Map<Node, Map<String, GRBVar>> community = new HashMap<>();
        if (community.containsKey(v)) {
          for (String comm : community.get(v).keySet()) {

            if (v.addedCommunity.contains(comm)||v.removedCommunity.contains(comm)) {
              continue;
            }
            // if parents add, then I will see it
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
          }

        }
        for (Node from : g.inboundNeighbors(v)) {
          GRBVar flowcons = flow.get(from).get(v);

          Edge thisEdge = g.getEdge(from, v);
          if (thisEdge.getType() ==  protocol.DEF && !g.defUsed.contains(thisEdge)) {
            model.addConstr(flowcons, GRB.EQUAL, 0.0, "defcons"+constraint);
            constraint = constraint + 1;                      
            continue;
          }


          if (thisEdge.getType() ==  protocol.IBGP) {
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

              /*GRBVar onlyIBGP = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, ("onlyIBGP"+constraint) );
              constraint = constraint + 1;
              tempAdd = new GRBLinExpr();
              tempAdd.addConstant(1.0);
              tempAdd.addTerm(-1.0, canReach);
              model.addConstr(onlyIBGP, GRB.EQUAL, tempAdd, "onlyIBGP"+constraint);
              constraint = constraint + 1;

              temp1.addTerm(-1.0, onlyIBGP);*/
              model.addConstr(flowcons, GRB.LESS_EQUAL, canReach, "reach-w/o-IBGP"+constraint);
              constraint = constraint + 1;

          }

          //*/
          //*
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

          tempAdd = new GRBLinExpr();
          tempAdd.addConstant(1.0);
          tempAdd.addTerm(-1.0, blockCom);
          model.addConstr(flowcons, GRB.LESS_EQUAL, tempAdd, "reach-w/o-IBGP"+constraint);
          constraint = constraint + 1;

        }

      }

      // Set objective:
      expr = new GRBLinExpr();
      for (GRBVar v : allFlows) {
        expr.addTerm(1.0, v);
      }
      if (length == 0) //min length
        model.setObjective(expr, GRB.MINIMIZE);
      else
        model.setObjective(expr, GRB.MAXIMIZE);

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
        /*
        System.out.println(x.get(GRB.StringAttr.VarName)
                           + " " +x.get(GRB.DoubleAttr.X));
        System.out.println(y.get(GRB.StringAttr.VarName)
                           + " " +y.get(GRB.DoubleAttr.X));
        System.out.println(z.get(GRB.StringAttr.VarName)
                           + " " +z.get(GRB.DoubleAttr.X));
        */

        if(model.get(GRB.IntAttr.Status) == GRB.Status.INFEASIBLE){
            //System.out.println("There is no optimal solution "+ model.get(GRB.IntAttr.Status));
            //model.computeIIS();
            //model.write("model.ilp");

        } else {                          

          //System.out.println("Obj: " + model.get(GRB.DoubleAttr.ObjVal));
          obj = model.get(GRB.DoubleAttr.ObjVal);
          //time = model.get(GRB.DoubleAttr.Runtime) * 1000;
          //System.out.println("ILP Time: " + time + " ms");

          // Dispose of model and environment
          //model.write("out.sol");
        }
        //
        //System.out.println("Number of variables: " + model.get(GRB.IntAttr.NumVars));
        //System.out.println("Number of constraints: " + model.get(GRB.IntAttr.NumConstrs));
        model.dispose();
        env.dispose();
    } catch (GRBException e) {
      System.out.println("Error code at optimization: " + e.getErrorCode() + ". " +
                         e.getMessage());
    }
    return time;
  }
}