package batfish.representation.cisco;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import batfish.grammar.cisco.CiscoGrammar.Interface_stanzaContext;
import batfish.grammar.cisco.CiscoGrammar.Ip_address_if_stanzaContext;
import batfish.representation.Ip;
import batfish.representation.SwitchportEncapsulationType;
import batfish.representation.SwitchportMode;
import batfish.util.SubRange;

public class Interface implements Serializable {

   private static final double FAST_ETHERNET_BANDWIDTH = 100E6;

   private static final double GIGABIT_ETHERNET_BANDWIDTH = 1E9;

   /**
    * dirty hack: just chose a very large number
    */
   private static final double LOOPBACK_BANDWIDTH = 1E12;
   private static final long serialVersionUID = 1L;
   private static final double TEN_GIGABIT_ETHERNET_BANDWIDTH = 10E9;

   public static double getDefaultBandwidth(String name) {
      Double bandwidth = null;
      if (name.startsWith("FastEthernet")) {
         bandwidth = FAST_ETHERNET_BANDWIDTH;
      }
      else if (name.startsWith("GigabitEthernet")) {
         bandwidth = GIGABIT_ETHERNET_BANDWIDTH;
      }
      else if (name.startsWith("TenGigabitEthernet")) {
         bandwidth = TEN_GIGABIT_ETHERNET_BANDWIDTH;
      }
      else if (name.startsWith("Vlan")) {
         bandwidth = null;
      }
      else if (name.startsWith("Loopback")) {
         bandwidth = LOOPBACK_BANDWIDTH;
      }
      if (bandwidth == null) {
         bandwidth = 1.0;
      }
      return bandwidth;
   }

   private int _accessVlan;
   private boolean _active;
   private ArrayList<SubRange> _allowedVlans;
   private Integer _area;
   private Double _bandwidth;
   private transient Interface_stanzaContext _context;
   private String _description;
   private String _incomingFilter;
   private Ip _ip;
   private transient Ip_address_if_stanzaContext _ipAddressStanzaContext;
   private String _name;
   private int _nativeVlan;
   private Integer _ospfCost;
   private int _ospfDeadInterval;
   private int _ospfHelloMultiplier;
   private String _outgoingFilter;
   private String _routingPolicy;
   private Map<String, String> _secondaryIps;

   private Ip _subnet;

   private SwitchportMode _switchportMode;

   private SwitchportEncapsulationType _switchportTrunkEncapsulation;

   public Interface(String name) {
      _name = name;
      _area = null;
      _ip = null;
      _active = true;
      _nativeVlan = 1;
      _switchportMode = SwitchportMode.NONE;
      _allowedVlans = new ArrayList<SubRange>();
      _secondaryIps = new HashMap<String, String>();
      _ospfCost = null;
   }

   public void addAllowedRanges(List<SubRange> ranges) {
      _allowedVlans.addAll(ranges);
   }

   public int getAccessVlan() {
      return _accessVlan;
   }

   public boolean getActive() {
      return _active;
   }

   public List<SubRange> getAllowedVlans() {
      return _allowedVlans;
   }

   public Integer getArea() {
      return _area;
   }

   public Double getBandwidth() {
      return _bandwidth;
   }

   public Interface_stanzaContext getContext() {
      return _context;
   }

   public String getDescription() {
      return _description;
   }

   public String getIncomingFilter() {
      return _incomingFilter;
   }

   public Ip getIP() {
      return _ip;
   }

   public Ip_address_if_stanzaContext getIpAddressStanzaContext() {
      return _ipAddressStanzaContext;
   }

   public String getName() {
      return _name;
   }

   public int getNativeVlan() {
      return _nativeVlan;
   }

   public Integer getOspfCost() {
      return _ospfCost;
   }

   public int getOspfDeadInterval() {
      return _ospfDeadInterval;
   }

   public int getOspfHelloMultiplier() {
      return _ospfHelloMultiplier;
   }

   public String getOutgoingFilter() {
      return _outgoingFilter;
   }

   public String getRoutingPolicy() {
      return _routingPolicy;
   }

   public Map<String, String> getSecondaryIps() {
      return _secondaryIps;
   }

   public Ip getSubnetMask() {
      return _subnet;
   }

   public SwitchportMode getSwitchportMode() {
      return _switchportMode;
   }

   public SwitchportEncapsulationType getSwitchportTrunkEncapsulation() {
      return _switchportTrunkEncapsulation;
   }

   public void setAccessVlan(int vlan) {
      _accessVlan = vlan;
   }

   public void setActive(boolean active) {
      _active = active;
   }

   public void setBandwidth(Double bandwidth) {
      _bandwidth = bandwidth;
   }

   public void setContext(Interface_stanzaContext context) {
      _context = context;
   }

   public void setDescription(String description) {
      _description = description;
   }

   public void setIncomingFilter(String accessListName) {
      _incomingFilter = accessListName;
   }

   public void setIp(Ip ip) {
      _ip = ip;
   }

   public void setIpAddressStanzaContext(Ip_address_if_stanzaContext ctx) {
      _ipAddressStanzaContext = ctx;
   }

   public void setNativeVlan(int vlan) {
      _nativeVlan = vlan;
   }

   public void setOspfCost(int ospfCost) {
      _ospfCost = ospfCost;
   }

   public void setOSPFDeadInterval(int seconds) {
      _ospfDeadInterval = seconds;
   }

   public void setOSPFHelloMultiplier(int multiplier) {
      _ospfHelloMultiplier = multiplier;
   }

   public void setOutgoingFilter(String accessListName) {
      _outgoingFilter = accessListName;
   }

   public void setRoutingPolicy(String routingPolicy) {
      _routingPolicy = routingPolicy;
   }

   public void setSubnetMask(Ip subnet) {
      _subnet = subnet;
   }

   public void setSwitchportMode(SwitchportMode switchportMode) {
      _switchportMode = switchportMode;
   }

   public void setSwitchportTrunkEncapsulation(
         SwitchportEncapsulationType encapsulation) {
      _switchportTrunkEncapsulation = encapsulation;
   }

}
