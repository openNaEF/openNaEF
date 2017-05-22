package voss.nms.inventory.util;

import naef.dto.NodeDto;
import naef.dto.PortDto;
import naef.dto.vlan.VlanDto;
import naef.dto.vlan.VlanIdPoolDto;
import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.link.Link;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.util.Util;

public class VlanUtil extends voss.core.server.util.VlanUtil {

    public static VlanIdPoolDto getPool(PageParameters param) throws ExternalServiceException {
        String poolID = param.getString(KEY_POOL_ID);
        if (poolID == null) {
            return null;
        }
        poolID = Util.decodeUTF8(poolID);
        return getPool(poolID);
    }

    public static Link<Void> createVlanEditLink(final String id, final WebPage backPage, final VlanDto vlan) {
        Link<Void> link = new Link<Void>(id) {
            private static final long serialVersionUID = 1L;

            public void onClick() {
                throw new IllegalStateException("not supported.");
            }
        };
        link.setEnabled(vlan != null);
        return link;
    }

    public static VlanDto getVlan(PageParameters param) throws ExternalServiceException {
        VlanIdPoolDto pool = getPool(param);
        String vlanId = param.getString(KEY_VLAN_ID);
        for (VlanDto vlan : pool.getUsers()) {
            if (vlan.getVlanId() != null && vlan.getVlanId().toString().equals(vlanId)) {
                return vlan;
            }
        }
        return null;
    }

    public static PageParameters getParameters(VlanIdPoolDto pool) {
        if (pool == null) {
            throw new IllegalArgumentException("pool is null.");
        }
        PageParameters param = new PageParameters();
        param.add(KEY_POOL_ID, Util.encodeUTF8(pool.getName()));
        return param;
    }

    public static PageParameters getParameters(VlanIdPoolDto pool, VlanDto vlan) {
        if (pool == null) {
            throw new IllegalArgumentException("pool is null.");
        } else if (vlan == null) {
            throw new IllegalArgumentException("vlan is null.");
        }
        PageParameters param = new PageParameters();
        param.add(KEY_POOL_ID, Util.encodeUTF8(pool.getName()));
        param.add(KEY_VLAN_ID, vlan.getVlanId().toString());
        return param;
    }


    public static Link<Void> createPublishButton(String id,
                                                 final NodeDto node, final PortDto port) {
        Link<Void> link = new Link<Void>(id) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick() {
                throw new IllegalStateException("not supported.");
            }
        };
        return link;
    }
}