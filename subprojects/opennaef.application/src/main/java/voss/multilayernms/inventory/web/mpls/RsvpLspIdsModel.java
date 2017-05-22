package voss.multilayernms.inventory.web.mpls;

import naef.dto.mpls.RsvpLspDto;
import naef.dto.mpls.RsvpLspIdPoolDto;
import org.apache.wicket.model.AbstractReadOnlyModel;

import java.util.*;

public class RsvpLspIdsModel extends AbstractReadOnlyModel<List<String>> {
    private static final long serialVersionUID = 1L;
    private final List<String> result = new ArrayList<String>();
    private final List<RsvpLspDto> users = new ArrayList<RsvpLspDto>();
    private final Map<String, RsvpLspDto> lsps = new HashMap<String, RsvpLspDto>();
    private final RsvpLspIdPoolDto pool;

    public RsvpLspIdsModel(RsvpLspIdPoolDto pool) {
        this.pool = pool;
    }

    @Override
    public List<String> getObject() {
        return result;
    }

    public List<RsvpLspDto> getUsers() {
        return this.users;
    }

    public Map<String, RsvpLspDto> getLspsMap() {
        return this.lsps;
    }

    public void renew() {
        List<String> lspNames = new ArrayList<String>();
        if (pool == null) {
            return;
        }
        pool.renew();
        this.users.clear();
        this.users.addAll(pool.getUsers());
        for (RsvpLspDto lsp : this.users) {
            lspNames.add(lsp.getName());
        }
        Collections.sort(lspNames);
        this.lsps.clear();
        for (RsvpLspDto lsp : this.users) {
            this.lsps.put(lsp.getName(), lsp);
        }
        this.result.clear();
        this.result.addAll(lspNames);
    }
}