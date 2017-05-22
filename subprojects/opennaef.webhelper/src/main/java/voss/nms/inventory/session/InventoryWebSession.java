package voss.nms.inventory.session;

import org.apache.wicket.Request;
import org.apache.wicket.protocol.http.WebSession;

public class InventoryWebSession extends WebSession {
    private static final long serialVersionUID = 1L;

    private boolean showAllVlans = false;
    private boolean showAllVplses = false;
    private boolean showAllVrfs = false;
    private boolean showAllPseudoWires = false;

    public InventoryWebSession(Request request) {
        super(request);
    }

    public boolean isShowAllVlans() {
        return showAllVlans;
    }

    public void setShowAllVlans(boolean showAllVlans) {
        this.showAllVlans = showAllVlans;
    }

    public boolean isShowAllVplses() {
        return showAllVplses;
    }

    public void setShowAllVplses(boolean showAllVplses) {
        this.showAllVplses = showAllVplses;
    }

    public boolean isShowAllVrfs() {
        return showAllVrfs;
    }

    public void setShowAllVrfs(boolean showAllVrfs) {
        this.showAllVrfs = showAllVrfs;
    }

    public boolean isShowAllPseudoWires() {
        return showAllPseudoWires;
    }

    public void setShowAllPseudoWires(boolean showAllPseudoWires) {
        this.showAllPseudoWires = showAllPseudoWires;
    }

}