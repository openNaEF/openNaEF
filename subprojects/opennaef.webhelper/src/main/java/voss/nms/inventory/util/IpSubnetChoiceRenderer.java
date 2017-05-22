package voss.nms.inventory.util;

import naef.dto.NodeDto;
import naef.dto.NodeElementDto;
import naef.dto.PortDto;
import naef.dto.ip.IpSubnetDto;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.NodeElementComparator;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IpSubnetChoiceRenderer extends ChoiceRenderer<IpSubnetDto> {
    private static final long serialVersionUID = 1L;

    @Override
    public Object getDisplayValue(IpSubnetDto subnet) {
        if (subnet == null) {
            return null;
        }
        List<PortDto> members = new ArrayList<PortDto>(subnet.getMemberIpifs());
        Collections.sort(members, new NodeElementComparator());
        StringBuilder sb = new StringBuilder();
        for (PortDto member : members) {
            if (sb.length() > 0) {
                sb.append(" <=> ");
            }
            NodeElementDto owner = member.getOwner();
            String ipAddress = DtoUtil.getStringOrNull(member, MPLSNMS_ATTR.IP_ADDRESS);
            String caption = null;
            if (owner == null) {
                throw new IllegalStateException();
            } else if (owner instanceof NodeDto) {
                caption = ((NodeDto) owner).getName() + "(" + ipAddress + ")";
            } else if (owner instanceof PortDto) {
                caption = NameUtil.getNodeIfName(owner) + "(" + ipAddress + ")";
            }
            sb.append(caption);
        }
        return sb.toString();
    }
}