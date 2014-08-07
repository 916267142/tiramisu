package batfish.representation.juniper;

import java.util.List;

public class ASPathAccessList {
   private List<ASPathAccessListLine> _lines;
   private String _name;

   public ASPathAccessList(String name, List<ASPathAccessListLine> lines) {
      _lines = lines;
      _name = name;
   }

   public List<ASPathAccessListLine> getLines() {
      return _lines;
   }

   public String getName() {
      return _name;
   }

}
