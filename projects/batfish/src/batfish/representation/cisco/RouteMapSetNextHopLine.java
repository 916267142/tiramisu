package batfish.representation.cisco;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import batfish.representation.Configuration;
import batfish.representation.Ip;
import batfish.representation.PolicyMapSetLine;
import batfish.representation.PolicyMapSetNextHopLine;

public class RouteMapSetNextHopLine extends RouteMapSetLine {

   private static final long serialVersionUID = 1L;

   private Set<Ip> _nextHops;

   public RouteMapSetNextHopLine(Set<Ip> nextHops) {
      _nextHops = nextHops;
   }

   public Set<Ip> getNextHops() {
      return _nextHops;
   }

   @Override
   public PolicyMapSetLine toPolicyMapSetLine(Configuration c) {
      // TODO: change to set in PolicyMapSetNextHopLine if possible
      List<Ip> nextHopList = new ArrayList<Ip>();
      nextHopList.addAll(_nextHops);
      return new PolicyMapSetNextHopLine(nextHopList);
   }

   @Override
   public RouteMapSetType getType() {
      return RouteMapSetType.NEXT_HOP;
   }

   @Override
   public boolean equalsRepresentation(Object o) {
      if (((RouteMapSetLine) o).getType() != RouteMapSetType.NEXT_HOP) {
         return false;
      }

      RouteMapSetNextHopLine rhsLine = (RouteMapSetNextHopLine) o;
      return getNextHops().equals(rhsLine.getNextHops());
   }
}
