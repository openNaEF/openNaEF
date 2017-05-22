package voss.discovery.agent.netscreen;


import voss.discovery.agent.common.ExtInfoRenderer;
import voss.model.ConfigurationExtInfo;
import voss.model.NetScreenZone;
import voss.model.VlanModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NetScreenZoneRenderer implements ExtInfoRenderer<List<NetScreenZone>> {
    private final VlanModel model;
    private List<NetScreenZone> object;

    @SuppressWarnings("unchecked")
    public NetScreenZoneRenderer(VlanModel model) {
        if (model == null) {
            throw new IllegalArgumentException();
        }
        ConfigurationExtInfo extInfo = model.gainConfigurationExtInfo();
        Object o = extInfo.get(NetScreenZone.KEY);
        if (o != null && !List.class.isInstance(o)) {
            throw new IllegalArgumentException("config-ext-info[" + NetScreenZone.KEY + "] is not list.");
        }
        this.model = model;
        this.object = (List<NetScreenZone>) o;
    }

    @Override
    public String getKey() {
        return NetScreenZone.KEY;
    }

    @Override
    public boolean isDefined() {
        return this.object != null;
    }

    @Override
    public void set(List<NetScreenZone> zones) {
        if (zones == null) {
            zones = Collections.emptyList();
        }
        this.model.gainConfigurationExtInfo().put(NetScreenZone.KEY, zones);
    }

    @Override
    public List<NetScreenZone> get() {
        if (this.object == null) {
            return new ArrayList<NetScreenZone>();
        }
        return object;
    }

    public NetScreenZone getById(int id) {
        if (this.object == null) {
            return null;
        }
        for (NetScreenZone zone : this.object) {
            if (zone.getZoneId() == id) {
                return zone;
            }
        }
        return null;
    }

    public NetScreenZone getByName(String name) {
        if (this.object == null) {
            return null;
        }
        for (NetScreenZone zone : this.object) {
            if (zone.getZoneName().equals(name)) {
                return zone;
            }
        }
        return null;
    }
}