package org.batfish.mulgraph;

public class EdgeCost {
    int AD;
    int ospf_cost;
    int as_length;
    int lp;
    int med;
    int rediscost;
    int weight;
    Boolean valid;

    public EdgeCost() {
        // 2147483 = max value
        AD = 2147483; //
        ospf_cost = 0;
        as_length = 0;
        lp = -1; //
        med = -1; //           
        rediscost = 0;
        valid = true;
    }

    public EdgeCost copy() {
        EdgeCost c = new EdgeCost();
        c.AD = AD; //
        c.ospf_cost = ospf_cost;
        c.as_length = as_length;
        c.lp = lp; //
        c.med = med; //           
        c.rediscost = rediscost;
        c.valid = valid;
        c.weight = weight;
        return c;
    }

    public void setAD(int cost) {
        AD = cost;
        valid = true;
    }

    public int getAD() {
        return AD;
    }

    public void setOSPF(int cost) {
        ospf_cost = cost;
        valid = true;
    }

    public int getOSPF() {
        return ospf_cost;
    }

    public void setAS(int cost) {
        as_length = cost;
        valid = true;
    }

    public int getAS() {
        return as_length;
    }

    public void setLP(int cost) {
        lp = cost;
        valid = true;
    }

    public int getLP() {
        return lp;
    }

    public void setMED(int cost) {
        med = cost;
        valid = true;
    }

    public int getMED() {
        return med;
    }

    public void setRedis(int cost) {
        rediscost = cost;
        valid = true;
    }

    public int getRedis() {
        return rediscost;
    }

    public void setWeight(int cost) {
        weight = cost;
        valid = true;
    }

    public int getWeight() {
        return weight;
    }

    public void setInvalid() {
        valid = false;   
    }

    @Override
    public String toString() {
        //return "Edge-Cost:" + valid + " (AD=" + AD + ", ospf=" + ospf_cost + ", AS=" + as_length + ", LP=" + lp + ", MED=" + med + ", REDIS=" + rediscost + ")";
        return "Edge-Cost:" + " (AD=" + AD + ", ospf=" + ospf_cost + ", AS=" + as_length + ", LP=" + lp +  ", MED=" + med + 
         ", weight=" + weight + ")";
        //return "Edge-Cost:";
    }

}
