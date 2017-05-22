package voss.model;


import java.util.ArrayList;
import java.util.List;

public class LabelSwitchedPathEndPoint extends AbstractLogicalPort {
    private static final long serialVersionUID = 1L;

    private final List<String> lspHops = new ArrayList<String>();
    private String lspName;
    private boolean lspEnable;
    private boolean inOperation;

    public void setLspName(String name) {
        this.lspName = name;
    }

    public String getLspName() {
        return this.lspName;
    }

    public void setLspStatus(boolean status) {
        this.lspEnable = status;
    }

    public boolean getLspStatus() {
        return this.lspEnable;
    }

    public boolean isEnable() {
        return this.lspEnable;
    }

    public void setInOperation(boolean status) {
        this.inOperation = status;
    }

    public boolean getInOperation() {
        return this.inOperation;
    }

    public boolean isInOperation() {
        return this.inOperation;
    }

    public void addLspHop(String hop) {
        lspHops.add(hop);
    }

    public List<String> getLspHops() {
        List<String> result = new ArrayList<String>();
        result.addAll(lspHops);
        return result;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(getDevice().getDeviceName());
        sb.append(":LSP:");
        sb.append(this.lspName);
        sb.append(",");
        sb.append((this.lspEnable == true ? "enable" : "disable"));
        sb.append(",");
        sb.append((this.inOperation == true ? "In Operation" : "Not in operation"));
        sb.append(",hop=");
        sb.append(this.lspHops);
        return sb.toString();
    }


}