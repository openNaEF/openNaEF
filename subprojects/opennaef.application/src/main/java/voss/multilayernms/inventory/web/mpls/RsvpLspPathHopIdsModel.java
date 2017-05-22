package voss.multilayernms.inventory.web.mpls;

import naef.dto.mpls.RsvpLspHopSeriesDto;
import naef.dto.mpls.RsvpLspHopSeriesIdPoolDto;
import org.apache.wicket.model.AbstractReadOnlyModel;

import java.util.*;

public class RsvpLspPathHopIdsModel extends AbstractReadOnlyModel<List<String>> {
    private static final long serialVersionUID = 1L;
    private final List<String> result = new ArrayList<String>();
    private final List<RsvpLspHopSeriesDto> users = new ArrayList<RsvpLspHopSeriesDto>();
    private final Map<String, RsvpLspHopSeriesDto> lsps = new HashMap<String, RsvpLspHopSeriesDto>();
    private final RsvpLspHopSeriesIdPoolDto pool;

    public RsvpLspPathHopIdsModel(RsvpLspHopSeriesIdPoolDto pool) {
        this.pool = pool;
    }

    @Override
    public List<String> getObject() {
        return result;
    }

    public List<RsvpLspHopSeriesDto> getUsers() {
        return this.users;
    }

    public Map<String, RsvpLspHopSeriesDto> getPathMap() {
        return this.lsps;
    }

    public void renew() {
        List<String> lspNames = new ArrayList<String>();
        if (pool == null) {
            return;
        }
        pool.renew();
        this.users.clear();
        this.lsps.clear();
        this.users.addAll(pool.getUsers());
        for (RsvpLspHopSeriesDto lsp : this.users) {
            lspNames.add(lsp.getName());
            this.lsps.put(lsp.getName(), lsp);
        }
        Collections.sort(lspNames);
        this.result.clear();
        this.result.addAll(lspNames);
    }
}