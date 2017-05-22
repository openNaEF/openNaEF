package voss.multilayernms.inventory.web.node;

import naef.dto.PortDto;
import naef.dto.atm.AtmApsIfDto;
import naef.dto.atm.AtmPortDto;
import naef.dto.eth.EthLagIfDto;
import naef.dto.eth.EthPortDto;
import naef.dto.pos.PosApsIfDto;
import naef.dto.pos.PosPortDto;
import naef.dto.serial.SerialPortDto;
import org.apache.wicket.markup.html.WebPage;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.builder.ShellCommands;
import voss.core.server.database.ShellConnector;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.ExceptionUtils;
import voss.nms.inventory.builder.SimpleNodeBuilder;
import voss.nms.inventory.util.AtmPvcUtil;

public class SubInterfaceLogic {

    private SubInterfaceLogic() {
    }

    public static boolean isSubInterfaceEnable(PortDto port) {
        if (port instanceof EthPortDto || port instanceof EthLagIfDto) {
            return true;
        } else if (port instanceof AtmPortDto || port instanceof AtmApsIfDto) {
            return true;
        } else if (port instanceof SerialPortDto || port instanceof PosPortDto || port instanceof PosApsIfDto) {
            return true;
        }
        return false;
    }

    public static WebPage getSubInterfacePage(PortDto port, WebPage backPage,
                                              String editorName) throws ExternalServiceException {
        if (!isSubInterfaceEnable(port)) {
            return null;
        }
        try {
            ShellCommands commands = new ShellCommands(editorName);
            if (port instanceof EthPortDto) {
                InventoryBuilder.changeContext(commands, port);
                SimpleNodeBuilder.buildVlanFeatureEnableCommands(commands);
                commands.addLastEditCommands();
                commands.addVersionCheckTarget(port);
                ShellConnector.getInstance().execute2(commands);
                port.renew();
                WebPage page = new VlanIfEditPage(backPage, (EthPortDto) port, null);
                return page;
            } else if (port instanceof EthLagIfDto) {
                InventoryBuilder.changeContext(commands, port);
                SimpleNodeBuilder.buildVlanFeatureEnableCommands(commands);
                commands.addLastEditCommands();
                commands.addVersionCheckTarget(port);
                ShellConnector.getInstance().execute2(commands);
                port.renew();
                WebPage page = new VlanIfEditPage(backPage, (EthLagIfDto) port, null);
                return page;
            } else if (port instanceof AtmPortDto || port instanceof AtmApsIfDto) {
                if (!AtmPvcUtil.isAtmPvcEnabled(port)) {
                    InventoryBuilder.changeContext(commands, port);
                    SimpleNodeBuilder.buildAtmPvcFeatureEnableCommand(commands);
                    commands.addLastEditCommands();
                    commands.addVersionCheckTarget(port);
                    ShellConnector.getInstance().execute2(commands);
                }
                return new AtmPvcEditPage(backPage, port.getNode(), port, null);
            } else if (port instanceof SerialPortDto || port instanceof PosPortDto || port instanceof PosApsIfDto) {
                return new ChannelPortEditPage(backPage, port.getNode(), port, null);
            }
            return null;
        } catch (Exception e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    public static String getCaption(PortDto port) throws InventoryException {
        if (!isSubInterfaceEnable(port)) {
            return null;
        }
        if (port instanceof EthPortDto || port instanceof EthLagIfDto) {
            return "[+VLAN]";
        } else if (port instanceof AtmPortDto || port instanceof AtmApsIfDto) {
            return "[+ATM]";
        } else if (port instanceof SerialPortDto || port instanceof PosPortDto || port instanceof PosApsIfDto) {
            return "[+Ch]";
        }
        return null;
    }
}