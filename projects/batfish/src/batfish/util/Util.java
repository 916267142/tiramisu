package batfish.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.antlr.v4.runtime.ParserRuleContext;

import batfish.representation.RepresentationObject;

public class Util {
   public static final String FACT_BLOCK_FOOTER = "\n//FACTS END HERE\n"
         + "   }) // clauses\n" + "} <-- .\n";

   public static String applyPrefix(String prefix, String msg) {
      String[] lines = msg.split("\n");
      StringBuilder sb = new StringBuilder();
      for (String line : lines) {
         sb.append(prefix + line + "\n");
      }
      return sb.toString();
   }

   public static boolean equalOrNull(Object lhs, Object rhs) {
      if (lhs == null && rhs == null) {
         return true;
      }
      if (lhs == null || rhs == null) {
         return false;
      }
      else {
         if (lhs instanceof RepresentationObject
               && rhs instanceof RepresentationObject)
            return ((RepresentationObject) lhs)
                  .equalsRepresentation((RepresentationObject) rhs);
         else
            return lhs.equals(rhs);

      }
   }

   public static <E> int cmpRepresentationLists(java.util.List<E> c1,
         java.util.List<E> c2) {
      if (c1 == null && c2 == null)
         return 0;
      if (c1 == null )
         return 1;
      if (c2 == null)
         return 2;
      if (c1.size() < c2.size())
         return 1;
      if (c1.size() > c2.size())
         return 2;

      Iterator<E> it2 = c2.iterator();
      for (Iterator<E> it = c1.iterator(); it.hasNext();) {
         if (!equalOrNull((E) it.next(), (E) it2.next()))
            return 3;
      }

      return 0;
   }

   public static <E> int cmpRepresentationSets(Set<E> c1, Set<E> c2) {
      if (c1 == null && c2 == null)
         return 0;
      if (c1 == null )
         return 1;
      if (c2 == null)
         return 2;
      if (c1.size() < c2.size())
         return 1;
      if (c1.size() > c2.size())
         return 2;

      Iterator<E> it2 = c2.iterator();
      for (Iterator<E> it = c1.iterator(); it.hasNext();) {
         if (!equalOrNull((E) it.next(), (E) it2.next()))
            return 3;
      }

      return 0;
   }

   public static <K, V> int cmpRepresentationMaps(Map<K, V> c1,
         Map<K, V> c2) {
      if (c1 == null && c2 == null)
         return 0;
      if (c1 == null )
         return 1;
      if (c2 == null)
         return 2;
      if (c1.size() < c2.size())
         return 1;
      if (c1.size() > c2.size())
         return 2;

      Iterator<Entry<K, V>> it2 = c2.entrySet().iterator();
      for (Iterator<Entry<K, V>> it = c1.entrySet().iterator(); it.hasNext();) {
         Entry<K, V> e1 = it.next();
         Entry<K, V> e2 = it2.next();
         if (!equalOrNull(e1.getKey(), e2.getKey())
               || !equalOrNull(e1.getValue(), e2.getValue()))
            return 3;
      }

      return 0;
   }

   public static String clearDuplicateLines(String input) {
      String[] lines = input.split("\\n");
      LinkedHashSet<String> lineSet = new LinkedHashSet<String>(lines.length);
      for (int i = 0; i < lines.length; i++) {
         String line = lines[i];
         lineSet.add(line);
      }
      StringBuilder writer = new StringBuilder(lineSet.toString().length());
      for (String line : lineSet) {
         writer.append(line + "\n");
      }
      return writer.toString();
   }

   public static String createFactBlockHeader(String blockName, String[] modules) {
      String output = "block (`" + blockName + ") {\n" + "   inactive(),\n";
      for (String module : modules) {
         output += "   alias_all(`" + module + "),\n";
      }
      output += "   clauses(`{\n" + "// FACTS START HERE\n\n";
      return output;
   }

   public static String extractBits(long l, int start, int end) {
      String s = "";
      for (int pos = end; pos >= start; pos--) {
         long mask = 1L << pos;
         long bit = l & mask;
         s += (bit != 0) ? 1 : 0;
      }
      return s;
   }
   
   public static String getIndentString(int indentLevel) {
	   
	   String retString = "";
	   
	   for (int i=0; i< indentLevel; i++) {
		   retString += "  ";
	   }   
	   
	   return retString;
   }

   public static String getIpFromIpSubnetPair(String pair) {
      int slashPos = pair.indexOf('/');
      return pair.substring(0, slashPos);
   }

   public static long getNetworkEnd(long networkStart, int prefix_length) {
      long networkEnd = networkStart;
      int ones_length = 32 - prefix_length;
      for (int i = 0; i < ones_length; i++) {
         networkEnd |= ((long) 1 << i);
      }
      return networkEnd;
   }

   public static String getPortName(int port) {
      switch (port) {
      case 0:
         return "any";
      case 179:
         return "bgp";
      case 68:
         return "bootpc";
      case 67:
         return "bootps";
      case 53:
         return "dns";
      case 21:
         return "ftp";
      case 20:
         return "ftp-data";
      case 500:
         return "isakmp";
      case 515:
         return "lpd";
      case 138:
         return "netbios-dgm";
      case 137:
         return "netbios-ns";
      case 139:
         return "netbios-ss";
      case 4500:
         return "non500-isakmp";
      case 123:
         return "ntp";
      case 496:
         return "pim-auto-rp";
      case 161:
         return "snmp";
      case 49:
         return "tacacs";
      case 23:
         return "telnet";
      case 69:
         return "tftp";
      case 80:
         return "www";
      default:
         return "" + port;
      }
   }
   
   public static int getPrefixLengthFromIpSubnetPair(String pair) {
      int slashPos = pair.indexOf('/');
      return Integer.parseInt(pair.substring(slashPos + 1, pair.length()));
   }
   
   public static String getProtocolName(int protocol) {
      switch (protocol) {
      case 0:
         return "ip";
      case 50:
         return "esp";
      case 47:
         return "gre";
      case 1:
         return "icmp";
      case 2:
         return "igmp";
      case 89:
         return "ospf";
      case 103:
         return "pim";
      case 132:
         return "sctp";
      case 6:
         return "tcp";
      case 17:
         return "udp";
      default:
         return "" + protocol;
      }
   }

   public static int getSubnetDivisor(String string) {
      return (1 << (32 - Util.numSubnetBits(string)));
   }

   public static String getText(ParserRuleContext ctx, String srcText) {
      int start = ctx.start.getStartIndex();
      int stop = ctx.stop.getStopIndex();
      return srcText.substring(start, stop);
   }

   public static long ipToLong(String addr) {
      String[] addrArray = addr.split("\\.");
      long num = 0;
      for (int i = 0; i < addrArray.length; i++) {
         int power = 3 - i;
         num += ((Integer.parseInt(addrArray[i]) % 256 * Math.pow(256, power)));
      }
      return num;
   }

   public static boolean isLoopback(String interfaceName) {
      return (interfaceName.startsWith("Loopback") || interfaceName
            .startsWith("lo"));
   }

   public static String longToCommunity(Long l) {
      Long upper = l >> 16;
      Long lower = l & 0xFFFF;
      return upper.toString() + ":" + lower;
   }

   public static String longToIp(long i) {
      return ((i >> 24) & 0xFF) + "." + ((i >> 16) & 0xFF) + "."
            + ((i >> 8) & 0xFF) + "." + (i & 0xFF);
   }

   public static String longToZ3Hex16(long l) {
      return "#x" + String.format("%04x", l);
   }

   public static String longToZ3Hex32(long l) {
      return "#x" + String.format("%08x", l);
   }

   public static int numSubnetBits(String subnet) {
      int count = 0;
      long subnetVal = Util.ipToLong(subnet);
      int subnetInt = (int) subnetVal;
      while (subnetInt != 0) {
         subnetInt <<= 1;
         count++;
      }
      return count;
   }

   public static long numSubnetBitsToSubnetLong(int numBits) {
      long val = 0;
      for (int i = 31; i > 31 - numBits; i--) {
         val |= ((long) 1 << i);
      }
      return val;
   }

   public static int numWildcardBits(long wildcard) {
      int numBits = 0;
      for (long test = wildcard; test != 0; test >>= 1) {
         numBits++;
      }
      return numBits;
   }

   public static long numWildcardBitsToWildcardLong(int numBits) {
      long wildcard = 0;
      for (int i = 0; i < numBits; i++) {
         wildcard |= (1 << i);
      }
      return wildcard;
   }
   
   public static String toHSAInterfaceName(String name) {
      if (name.startsWith("xe-")) {
         String numberSection = name.substring(3);
         String[] numbers = numberSection.split("/"); // should be three
         return ("te" + numbers[0] + numbers[1] + numbers[2]).replace(".", "/");
      }
      else if (name.startsWith("irb")) {
         String numberSection = name.substring(4);
         return "irb" + numberSection + "/0";
      }
      else if (name.startsWith("lo")) {
         String numberSection = name.substring(2);
         String[] numbers = numberSection.split("\\.");
         return "loopback" + numbers[0] + numbers[1];
      }
      else if (name.startsWith("fxp")) {
         String numberSection = name.substring(3);
         String[] numbers = numberSection.split("\\.");
         return "fxp" + numbers[0] + "/" + numbers[1];         
      }
      else if (name.startsWith("Vlan")) {
         return name.replace("Vlan","Flan") + "/0";
      }
      else if (name.startsWith("Port-channel")) {
         return name.replace("Port-channel", "pc") + "/0";
      }
      else if (name.startsWith("TenGigabitEthernet")) {
         return name.replace("TenGigabitEthernet", "te");
      }
      else if (name.startsWith("GigabitEthernet")) {
         return name.replace("GigabitEthernet", "ge");
      }
      else if (name.startsWith("FastEthernet")) {
         return name.replace("FastEthernet", "fe");
      }
      else {
         return name;
      }
   }

   private Util() {
   }

   public int nCr(int n, int r) {
      int product = 1;
      int rPrime;
      if (r < n / 2) {
         rPrime = n - r;
      }
      else {
         rPrime = r;
      }
      for (int i = n; i > rPrime; i--) {
         product *= i;
      }
      return product;
   }

}
