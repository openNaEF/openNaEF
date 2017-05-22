package voss.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class LspHopSeries implements Serializable {
    private static final long serialVersionUID = 1L;

    List<String> hops = new ArrayList<String>();

    public void addHop(String hop) {
        this.hops.add(hop);
    }

    public void clearHops() {
        this.hops.clear();
    }

    public List<String> getHops() {
        List<String> result = new ArrayList<String>();
        result.addAll(this.hops);
        return result;
    }

    public String toString() {
        StringBuffer sb = null;
        for (String hop : hops) {
            if (sb == null) {
                sb = new StringBuffer().append(hop);
            } else {
                sb.append("->").append(hop);
            }
        }
        if (sb != null) {
            return sb.toString();
        } else {
            return super.toString();
        }
    }

}