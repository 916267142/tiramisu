package org.batfish.mulgraph;

import gurobi.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

import org.ants.VeriBoost;
import org.ants.VeriBoostUtil;
import org.batfish.symbolic.smt.PropertyChecker;

import java.util.ArrayList;

public class ilpMinCut {

  Tpg g;
  GRBEnv env;
  GRBModel model;
  public double runTime = 0;
  public double obj = -1;

  public ilpMinCut(Tpg graph)
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
  
  public void formulate(TpgNode src, TpgNode dst) {
    try {
      // Create variables
      GRBLinExpr expr, inflow, tempAdd, temp1;
      Map<TpgNode, Map<TpgNode, GRBVar>> flow = new HashMap<>();
      Map<TpgEdge, GRBVar> fail = new HashMap<>();
      List<GRBVar> allFail = new ArrayList<>();
      Map<TpgNode, GRBVar> reachable = new HashMap<>();

      int constraint = 0;
      for (TpgNode from : g.getVertices()) {
        Map<TpgNode, GRBVar> flowTemp = new HashMap<>();
        for(TpgEdge to : g.getNeighbors(from)) {
          GRBVar flowVar = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, ("flow"+from+"-"+to.getDst()) );
          flowTemp.put(to.getDst(), flowVar);
        }
        flow.put(from, flowTemp);
      }

      for (TpgNode v : g.getVertices()) {
        GRBVar reach = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, ("reach"+v));
        reachable.put(v, reach);
      }

      for (Map.Entry<String,Set<TpgEdge>> entry : g.getPhysicalMap().entrySet()) {
        String skey = entry.getKey();
        Set<TpgEdge> valueEdgeSet = entry.getValue();
        GRBVar failKey = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, ("fail"+skey));
        // CHARLIE_ADD_BEGIN
        if(VeriBoost.isPrune) {
          int idx = skey.lastIndexOf("_");
          VeriBoostUtil.Interface from = new VeriBoostUtil.Interface(skey.substring(0, idx).toLowerCase(), "none");
          VeriBoostUtil.Interface to = new VeriBoostUtil.Interface(skey.substring(idx + 1).toLowerCase(), "none");
          VeriBoostUtil.Link link   = new VeriBoostUtil.Link(from, to);
          VeriBoostUtil.Link link2  = new VeriBoostUtil.Link(to, from);

          if(PropertyChecker.veriBoost.isLinkFree(link) || PropertyChecker.veriBoost.isLinkUp(link2)) {
            // System.out.println("Link " + skey + " is free");
            allFail.add(failKey);
          }
        } else {
          allFail.add(failKey);
        }
        // // CHARLIE_ADD_END
        for (TpgEdge tpgEdge : valueEdgeSet) {
          fail.put(tpgEdge, failKey);
        }
      }

      model.addConstr(reachable.get(src), GRB.EQUAL, 1.0, "reachcons"+constraint);
      constraint = constraint + 1;
      model.addConstr(reachable.get(dst), GRB.EQUAL, 0.0, "reachcons"+constraint);
      constraint = constraint + 1;

      // assume the graph which has been given is already pruned and tells me what is the src and dst node

      for (TpgNode v : g.getVertices()) {
        // change it to if-else stmt to avoid use of continue
        if (v == src) {
          continue;
        }

        inflow = new GRBLinExpr();

        // flow = reach - fail
        // reach = summation(flow)
        for (TpgNode from : g.inboundNeighbors(v)) {
          // // CHARLIE_ADD_BEGIN
          // if(VeriBoost.isPrune) {
          //   VeriBoostUtil.Interface fromInterface = new VeriBoostUtil.Interface(from.getDevice().toLowerCase());
          //   VeriBoostUtil.Interface toInterface = new VeriBoostUtil.Interface(v.getDevice().toLowerCase());
          //   VeriBoostUtil.Link link = new VeriBoostUtil.Link(fromInterface, toInterface);
          //   if (PropertyChecker.veriBoost.isLinkDown(link)) {
          //     continue; // Skip if the link is down
          //   }
          // }
          // // CHARLIE_ADD_END
          GRBVar flowcons = flow.get(from).get(v);
          TpgEdge thisEdge = g.getEdge(from, v);
          temp1 = new GRBLinExpr();
          temp1.addTerm(1.0, reachable.get(from));
          if (fail.containsKey(thisEdge)) {
            temp1.addTerm(-1.0, fail.get(thisEdge));
          }

          model.addConstr(flowcons, GRB.GREATER_EQUAL, temp1, "flowcons"+constraint);
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
      // CHARLIE_ADD_BEGIN
      if (VeriBoost.isPrune) {
          for (Map.Entry<TpgEdge, GRBVar> entry : fail.entrySet()) {
              TpgEdge edge = entry.getKey();
              GRBVar var = entry.getValue();
              
              VeriBoostUtil.Interface from = new VeriBoostUtil.Interface(edge.getSrc().getDevice().toLowerCase());
              VeriBoostUtil.Interface to = new VeriBoostUtil.Interface(edge.getDst().getDevice().toLowerCase());
              VeriBoostUtil.Link link = new VeriBoostUtil.Link(from, to);
              
              if (PropertyChecker.veriBoost.isLinkUp(link)) {
                  try {
                      var.set(GRB.DoubleAttr.LB, 0.0);
                      var.set(GRB.DoubleAttr.UB, 0.0);
                  } catch (GRBException e) {
                      System.err.println("Error setting link to up (0): " + e.getMessage());
                  }
              } else if (PropertyChecker.veriBoost.isLinkDown(link)) {
                  try {
                      var.set(GRB.DoubleAttr.LB, 1.0);
                      var.set(GRB.DoubleAttr.UB, 1.0);
                  } catch (GRBException e) {
                      System.err.println("Error setting link to down (1): " + e.getMessage());
                  }
              }
          }
      }
      model.update();
      // CHARLIE_ADD_END
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