package batfish.representation.cisco;

import java.io.Serializable;

import batfish.representation.Ip;
import batfish.representation.RepresentationObject;
import batfish.util.Util;

public class OspfNetwork implements Comparable<OspfNetwork>, Serializable,
      RepresentationObject {

   private static final long serialVersionUID = 1L;

   private long _area;
   private int _hashCode;
   private Ip _networkAddress;
   private Ip _subnetMask;

   public OspfNetwork(Ip networkAddress, Ip subnetMask, long area) {
      _networkAddress = networkAddress;
      _subnetMask = subnetMask;
      _area = area;
      _hashCode = (networkAddress.networkString(_subnetMask) + ":" + _area)
            .hashCode();
   }

   @Override
   public int compareTo(OspfNetwork rhs) {
      int ret = _networkAddress.compareTo(rhs._networkAddress);
      if (ret == 0) {
         ret = _subnetMask.compareTo(rhs._subnetMask);
         if (ret == 0) {
            ret = Long.compare(_area, rhs._area);
         }
      }
      return ret;
   }

   @Override
   public boolean equals(Object o) {
      OspfNetwork rhs = (OspfNetwork) o;
      return _networkAddress.equals(rhs._networkAddress)
            && _subnetMask.equals(rhs._subnetMask) && _area == rhs._area;
   }

   public long getArea() {
      return _area;
   }

   public Ip getNetworkAddress() {
      return _networkAddress;
   }

   public Ip getSubnetMask() {
      return _subnetMask;
   }

   @Override
   public int hashCode() {
      return _hashCode;
   }

   @Override
   public boolean equalsRepresentation(Object o) {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public void diffRepresentation(Object o, String string, boolean reverse) {
      if (reverse) {
         System.out.println("+ " + string);
         System.out.println("+ " + string + "._area:" + _area);
         System.out.println("+ " + string + "._networkAddress:"
               + Util.objectToString(_networkAddress));
         System.out.println("+ " + string + "._subnetMask:"
               + Util.objectToString(_subnetMask));
         System.out.flush();
         return;
      }

      if (o == null) {
         System.out.println("- " + string);
         System.out.println("- " + string + "._area:" + _area);
         System.out.println("- " + string + "._networkAddress:"
               + Util.objectToString(_networkAddress));
         System.out.println("- " + string + "._subnetMask:"
               + Util.objectToString(_subnetMask));
         System.out.flush();
         return;
      }

      OspfNetwork rhs = (OspfNetwork) o;
      if (_area != rhs._area) {
         System.out.println("- " + string + "._area:" + _area);
         System.out.println("+ " + string + "._area:" + rhs._area);
      }
      if (!Util.equalOrNull(_networkAddress, rhs._networkAddress)) {
         System.out.println("- " + string + "._networkAddress:"
               + Util.objectToString(_networkAddress));
         System.out.println("+ " + string + "._networkAddress:"
               + Util.objectToString(rhs._networkAddress));
      }
      if (!Util.equalOrNull(_subnetMask, rhs._subnetMask)) {
         System.out.println("- " + string + "._subnetMask:"
               + Util.objectToString(_subnetMask));
         System.out.println("+ " + string + "._subnetMask:"
               + Util.objectToString(rhs._subnetMask));
      }
      System.out.flush();
      return;
   }

}
