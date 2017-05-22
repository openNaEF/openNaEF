package voss.multilayernms.inventory.nmscore.web.flow.state;

import naef.dto.IdRange;
import naef.dto.NodeDto;
import naef.dto.PortDto;
import naef.dto.ip.IpIfDto;
import naef.dto.ip.IpSubnetDto;
import naef.mvo.ip.IpAddress;
import net.phalanx.core.expressions.ObjectFilterQuery;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.database.ShellConnector;
import voss.core.server.exception.ExternalServiceException;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.nmscore.constraints.PortEditConstraints;
import voss.multilayernms.inventory.nmscore.web.flow.FlowContext;
import voss.multilayernms.inventory.nmscore.web.flow.Operation;
import voss.multilayernms.inventory.renderer.PortRenderer;
import voss.multilayernms.inventory.renderer.SubnetRenderer;
import voss.nms.inventory.builder.IpSubnetCommandBuilder;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class InventoryIpSubnetDeleteState extends UnificUIViewState {

    public InventoryIpSubnetDeleteState(StateId stateId) {
        super(stateId);
    }

    @Override
    public void execute(FlowContext context) throws ServletException {
        try {
            MplsNmsInventoryConnector conn = MplsNmsInventoryConnector.getInstance();
            List<CommandBuilder> commandBuilderList = new ArrayList<CommandBuilder>();

            List<IpIfDto> ipIfs = getAllIpIf(conn);
            List<String> targets = getMvoIdsFromQuery(context);
            String userName = context.getUser();

            for (String mvoId : targets) {
                IpSubnetDto target = conn.getMvoDto(mvoId, IpSubnetDto.class);
                if (!checkMemberIpIfs(target, ipIfs)) {
                    throw new IllegalArgumentException("An IP address has been paid out. :" + SubnetRenderer.getIpAddress(target) + "/" + SubnetRenderer.getSubnetMask(target));
                }

                IpSubnetCommandBuilder builder = new IpSubnetCommandBuilder(target, userName);
                BuildResult result = builder.buildDeleteCommand();
                if (result != BuildResult.SUCCESS) {
                    throw new IllegalArgumentException("Command generation failed.:" + SubnetRenderer.getIpAddress(target) + "/" + SubnetRenderer.getSubnetMask(target));
                }
                commandBuilderList.add(builder);
            }
            ShellConnector.getInstance().executes(commandBuilderList);
            super.execute(context);
        } catch (Exception e) {
            log.error("", e);
            throw new ServletException(e.getMessage());
        }
    }

    private boolean checkMemberIpIfs(IpSubnetDto target, List<IpIfDto> ipIfs) {
        if (target.getMemberIpifs().size() > 0 || target.getMemberPorts().size() > 0) {
            return false;
        }
        Set<IdRange<IpAddress>> ranges = target.getSubnetAddress().getIdRanges();
        for (IpIfDto ipIf : ipIfs) {
            IpAddress ip = IpAddress.gain(PortRenderer.getIpAddress(ipIf));
            for (IdRange<IpAddress> range : ranges) {
                IpAddress lower = range.lowerBound;
                IpAddress upper = range.upperBound;
                if (0 >= lower.compareTo(ip) && upper.compareTo(ip) >= 0) {
                    return false;
                }
            }
        }
        return true;
    }

    private List<IpIfDto> getAllIpIf(MplsNmsInventoryConnector conn) throws ExternalServiceException {
        List<IpIfDto> ipIfList = new ArrayList<IpIfDto>();

        for (NodeDto node : conn.getActiveNodes()) {
            for (PortDto port : node.getPorts()) {
                if (port instanceof IpIfDto) {
                    if (PortEditConstraints.isPortDirectLine(port)) {
                        ipIfList.add((IpIfDto) port);
                    }
                }
            }
        }
        return ipIfList;
    }

    @SuppressWarnings("unchecked")
    public static List<String> getMvoIdsFromQuery(FlowContext context) throws IOException {
        ObjectFilterQuery query = Operation.getQuery(context);
        if (!query.containsKey("ID")) {
            throw new IllegalArgumentException("Target is unknown.");
        }
        List<String> targets = null;
        Object target = query.get("ID").getPattern();
        if (target instanceof String) {
            targets = new ArrayList<String>();
            targets.add((String) target);
        } else if (target instanceof List) {
            targets = (List<String>) target;
        }
        return targets;
    }
}