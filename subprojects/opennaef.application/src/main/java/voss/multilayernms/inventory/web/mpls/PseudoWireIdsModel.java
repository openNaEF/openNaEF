package voss.multilayernms.inventory.web.mpls;

import naef.dto.mpls.PseudowireDto;
import naef.dto.mpls.PseudowireStringIdPoolDto;
import org.apache.wicket.model.AbstractReadOnlyModel;

import java.util.*;

public class PseudoWireIdsModel extends AbstractReadOnlyModel<List<String>> {
    private static final long serialVersionUID = 1L;
    private final List<String> result = new ArrayList<String>();
    private final List<PseudowireDto> users = new ArrayList<PseudowireDto>();
    private final Map<String, PseudowireDto> vcs = new HashMap<String, PseudowireDto>();
    private final PseudowireStringIdPoolDto pool;
    private boolean showAll = false;

    public PseudoWireIdsModel(PseudowireStringIdPoolDto pool) {
        this.pool = pool;
    }

    @Override
    public List<String> getObject() {
        return result;
    }

    public List<PseudowireDto> getUsers() {
        return this.users;
    }

    public boolean isShowAll() {
        return this.showAll;
    }

    public Map<String, PseudowireDto> getVcsMap() {
        return this.vcs;
    }

    public void renew() {
        renew(this.showAll);
    }

    public void renew(boolean showAll) {
        this.showAll = showAll;
        List<String> vcIDs = new ArrayList<String>();
        if (pool == null) {
            return;
        }
        pool.renew();
        this.users.clear();
        this.users.addAll(pool.getUsers());
        for (PseudowireDto pw : this.users) {
            vcIDs.add(pw.getStringId());
        }
        Collections.sort(vcIDs);
        this.vcs.clear();
        for (PseudowireDto pw : this.users) {
            this.vcs.put(pw.getStringId(), pw);
        }
        this.result.clear();
        this.result.addAll(vcIDs);
    }
}