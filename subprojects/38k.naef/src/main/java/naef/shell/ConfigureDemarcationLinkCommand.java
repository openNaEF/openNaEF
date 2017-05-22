package naef.shell;

import naef.mvo.Network;
import naef.mvo.Port;
import naef.mvo.atm.AtmApsIf;
import naef.mvo.atm.AtmApsLink;
import naef.mvo.atm.AtmLink;
import naef.mvo.atm.AtmPvc;
import naef.mvo.atm.AtmPvcIf;
import naef.mvo.atm.AtmPvp;
import naef.mvo.atm.AtmPvpIf;
import naef.mvo.eth.EthLag;
import naef.mvo.eth.EthLagIf;
import naef.mvo.eth.EthLink;
import naef.mvo.eth.EthPort;
import naef.mvo.fr.FrPvc;
import naef.mvo.fr.FrPvcIf;
import naef.mvo.pos.PosApsIf;
import naef.mvo.pos.PosApsLink;
import naef.mvo.pos.PosLink;
import naef.mvo.vlan.VlanIf;
import naef.mvo.vlan.VlanLink;
import tef.skelton.SkeltonTefService;

import java.util.Set;

public class ConfigureDemarcationLinkCommand extends NaefShellCommand {

    private enum Operation {

        SET_ETH_LINK(false) {

            @Override void process(Port contextPort, Port objectPort)
                throws ShellCommandException
            {
                if (objectPort != null) {
                    throw new IllegalArgumentException();
                }

                processSetEthLink(contextPort, EthPort.class);
            }
        },

        RESET_ETH_LINK(false)  {

            @Override void process(Port contextPort, Port objectPort)
                throws ShellCommandException
            {
                if (objectPort != null) {
                    throw new RuntimeException();
                }

                processResetEthLink(contextPort, EthPort.class);
            }
        },

        SET_ETH_LAG_LINK(false) {

            @Override void process(Port contextPort, Port objectPort)
                throws ShellCommandException
            {
                if (objectPort != null) {
                    throw new IllegalArgumentException();
                }

                processSetEthLink(contextPort, EthLagIf.class);
            }
        },

        RESET_ETH_LAG_LINK(false) {

            @Override void process(Port contextPort, Port objectPort)
                throws ShellCommandException
            {
                if (objectPort != null) {
                    throw new RuntimeException();
                }

                processResetEthLink(contextPort, EthLagIf.class);
            }
        },

        SET_TRUNK(true) {

            @Override void process(Port contextPort, Port objectPort)
                throws ShellCommandException
            {
                VlanIf vlanif = validatePortAs(contextPort, VlanIf.class, "コンテキスト");
                VlanLink demarcVlanlink = getVlanTrunkDemarcationLink(vlanif, objectPort);
                if (demarcVlanlink == null) {
                    demarcVlanlink = new VlanLink(vlanif, null);
                }

                EthType ethtype = EthType.get(objectPort, "引数");
                Network.LowerStackable ethlink = ethtype.getLink(objectPort, true);

                vlanif.joinNetwork(demarcVlanlink);

                demarcVlanlink.stackOver(ethlink);
            }
        },

        RESET_TRUNK(true) {

            @Override void process(Port contextPort, Port objectPort)
                throws ShellCommandException
            {
                VlanIf vlanif = validatePortAs(contextPort, VlanIf.class, "コンテキスト");
                VlanLink demarcVlanlink = getVlanTrunkDemarcationLink(vlanif, objectPort);
                if (demarcVlanlink == null) {
                    throw new ShellCommandException(
                        "境界リンクが接続されていません: " + vlanif.getFqn() + " " + objectPort.getFqn());
                }

                EthType ethtype = EthType.get(objectPort, "引数");
                Network.LowerStackable ethlink = ethtype.getLink(objectPort, true);

                demarcVlanlink.setContainer(null);
                demarcVlanlink.unstackOver(ethlink);
                vlanif.disjoinNetwork(demarcVlanlink);

                if (demarcVlanlink.getHereafterLowerLayers(false).size() != 0) {
                    throw new IllegalStateException(vlanif.getFqn());
                }
            }
        },

        BUNDLE_LAG_PORT(true) {

            @Override void process(Port contextPort, Port objectPort)
                throws ShellCommandException
            {
                EthLag laglink = (EthLag) EthType.LAG.getDemarcLink(
                    validatePortAs(contextPort, EthLagIf.class, "コンテキスト"),
                    true);
                EthLink ethlink = (EthLink) EthType.ETH.getDemarcLink(
                    validatePortAs(objectPort, EthPort.class, "引数"),
                    true);

                laglink.addPart(ethlink);
            }
        },

        UNBUNDLE_LAG_PORT(true) {

            @Override void process(Port contextPort, Port objectPort)
                throws ShellCommandException {
                EthLag laglink = (EthLag) EthType.LAG.getDemarcLink(
                    validatePortAs(contextPort, EthLagIf.class, "コンテキスト"),
                    true);
                EthLink ethlink = (EthLink) EthType.ETH.getDemarcLink(
                    validatePortAs(objectPort, EthPort.class, "引数"),
                    true);

                laglink.removePart(ethlink);
            }
        },

        SET_ATM_LINK(false) {

            @Override void process(Port contextPort, Port objectPort)
                throws ShellCommandException {
                if (objectPort != null) {
                    throw new IllegalArgumentException();
                }

                AtmLink demarcLink = selectDemarcLink(contextPort, AtmLink.class);
                if (demarcLink == null) {
                    demarcLink = new AtmLink(contextPort, null);
                }
                contextPort.joinNetwork(demarcLink);
            }
        },

        RESET_ATM_LINK(false) {

            @Override void process(Port contextPort, Port objectPort)
                throws ShellCommandException {
                if (objectPort != null) {
                    throw new IllegalArgumentException();
                }

                AtmLink demarcLink = selectDemarcLink(contextPort, AtmLink.class);
                if (demarcLink == null) {
                    throw new ShellCommandException("境界atm-linkが接続されていません.");
                }
                contextPort.disjoinNetwork(demarcLink);
            }
        },

        SET_ATM_APS_LINK(false) {

            @Override void process(Port contextPort, Port objectPort)
                throws ShellCommandException {
                AtmApsIf atmApsIf = validatePortAs(contextPort, AtmApsIf.class, "コンテキスト");
                AtmApsLink demarcLink = selectDemarcLink(atmApsIf, AtmApsLink.class);
                if (demarcLink == null) {
                    demarcLink = new AtmApsLink(atmApsIf, null);
                }
                atmApsIf.joinNetwork(demarcLink);
            }
        },

        RESET_ATM_APS_LINK(false) {

            @Override void process(Port contextPort, Port objectPort)
                throws ShellCommandException {
                AtmApsIf atmApsIf = validatePortAs(contextPort, AtmApsIf.class, "コンテキスト");
                AtmApsLink demarcLink = selectDemarcLink(atmApsIf, AtmApsLink.class);
                if (demarcLink == null) {
                    throw new ShellCommandException("境界atm-aps-linkが接続されていません.");
                }
                atmApsIf.disjoinNetwork(demarcLink);
            }
        },

        SET_ATM_PVC(false) {

            @Override void process(Port contextPort, Port objectPort)
                throws ShellCommandException {
                if (objectPort != null) {
                    throw new IllegalArgumentException();
                }

                AtmPvcIf pvcif = validatePortAs(contextPort, AtmPvcIf.class, "コンテキスト");
                AtmPvc demarcPvc = selectDemarcLink(pvcif, AtmPvc.class);
                if (demarcPvc == null) {
                    demarcPvc = new AtmPvc(pvcif, null);
                }
                pvcif.joinNetwork(demarcPvc);
            }
        },

        RESET_ATM_PVC(false) {

            @Override void process(Port contextPort, Port objectPort)
                throws ShellCommandException {
                if (objectPort != null) {
                    throw new IllegalArgumentException();
                }

                AtmPvcIf pvcif = validatePortAs(contextPort, AtmPvcIf.class, "コンテキスト");
                AtmPvc demarcPvc = selectDemarcLink(pvcif, AtmPvc.class);
                if (demarcPvc == null) {
                    throw new ShellCommandException("境界PVCが接続されていません.");
                }
                pvcif.disjoinNetwork(demarcPvc);
            }
        },

        SET_ATM_PVP(false) {

            @Override void process(Port contextPort, Port objectPort)
                throws ShellCommandException {
                if (objectPort != null) {
                    throw new IllegalArgumentException();
                }

                AtmPvpIf pvpif = validatePortAs(contextPort, AtmPvpIf.class, "コンテキスト");
                AtmPvp demarcPvp = selectDemarcLink(pvpif, AtmPvp.class);
                if (demarcPvp == null) {
                    demarcPvp = new AtmPvp(pvpif, null);
                }
                pvpif.joinNetwork(demarcPvp);
            }
        },

        RESET_ATM_PVP(false) {

            @Override void process(Port contextPort, Port objectPort)
                throws ShellCommandException {
                if (objectPort != null) {
                    throw new IllegalArgumentException();
                }

                AtmPvpIf pvpif = validatePortAs(contextPort, AtmPvpIf.class, "コンテキスト");
                AtmPvp demarcPvp = selectDemarcLink(pvpif, AtmPvp.class);
                if (demarcPvp == null) {
                    throw new ShellCommandException("境界PVPが接続されていません.");
                }
                pvpif.disjoinNetwork(demarcPvp);
            }
        },

        SET_FR_PVC(false) {

            @Override void process(Port contextPort, Port objectPort)
                throws ShellCommandException {
                if (objectPort != null) {
                    throw new IllegalArgumentException();
                }

                FrPvcIf pvcif = validatePortAs(contextPort, FrPvcIf.class, "コンテキスト");
                FrPvc demarcPvc = selectDemarcLink(pvcif, FrPvc.class);
                if (demarcPvc == null) {
                    demarcPvc = new FrPvc(pvcif, null);
                }
                pvcif.joinNetwork(demarcPvc);
            }
        },

        RESET_FR_PVC(false) {

            @Override void process(Port contextPort, Port objectPort)
                throws ShellCommandException {
                if (objectPort != null) {
                    throw new IllegalArgumentException();
                }

                FrPvcIf pvcif = validatePortAs(contextPort, FrPvcIf.class, "コンテキスト");
                FrPvc demarcPvc = selectDemarcLink(pvcif, FrPvc.class);
                if (demarcPvc == null) {
                    throw new ShellCommandException("境界PVCが接続されていません.");
                }
                pvcif.disjoinNetwork(demarcPvc);
            }
        },

        SET_POS_LINK(false) {

            @Override void process(Port contextPort, Port objectPort)
                throws ShellCommandException {
                PosLink demarcLink = selectDemarcLink(contextPort, PosLink.class);
                if (demarcLink == null) {
                    demarcLink = new PosLink(contextPort, null);
                }
                contextPort.joinNetwork(demarcLink);
            }
        },

        RESET_POS_LINK(false) {

            @Override void process(Port contextPort, Port objectPort)
                throws ShellCommandException {
                PosLink demarcLink = selectDemarcLink(contextPort, PosLink.class);
                if (demarcLink == null) {
                    throw new ShellCommandException("境界リンクが接続されていません.");
                }
                contextPort.disjoinNetwork(demarcLink);
            }
        },

        SET_POS_APS_LINK(false) {

            @Override void process(Port contextPort, Port objectPort)
                throws ShellCommandException {
                PosApsIf posApsIf = validatePortAs(contextPort, PosApsIf.class, "コンテキスト");
                PosApsLink demarcLink = selectDemarcLink(posApsIf, PosApsLink.class);
                if (demarcLink == null) {
                    demarcLink = new PosApsLink(posApsIf, null);
                }
                posApsIf.joinNetwork(demarcLink);
            }
        },

        RESET_POS_APS_LINK(false) {

            @Override void process(Port contextPort, Port objectPort)
                throws ShellCommandException {
                PosApsIf posApsIf = validatePortAs(contextPort, PosApsIf.class, "コンテキスト");
                PosApsLink demarcLink = selectDemarcLink(posApsIf, PosApsLink.class);
                if (demarcLink == null) {
                    throw new ShellCommandException("境界リンクが接続されていません.");
                }
                posApsIf.disjoinNetwork(demarcLink);
            }
        };

        private final boolean needsObjectPort_;

        Operation(boolean needsObjectPort) {
            needsObjectPort_ = needsObjectPort;
        }

        final boolean needsObjectPort() {
            return needsObjectPort_;
        }

        abstract void process(Port contextPort, Port objectPort) throws ShellCommandException;

        private static void processSetEthLink(Port port, Class<? extends Port> klass)
            throws ShellCommandException
        {
            validatePortAs(port, klass, "コンテキスト");

            EthType ethtype = EthType.get(port, "コンテキスト");
            Network.LowerStackable ethlink = ethtype.getDemarcLink(port, false);
            if (ethlink == null) {
                ethlink = ethtype.newDemarcLink(port);
            }

            port.joinNetwork(ethlink);
        }

        private static void processResetEthLink(Port port, Class<? extends Port> klass)
            throws ShellCommandException
        {
            validatePortAs(port, klass, "コンテキスト");

            EthType ethtype = EthType.get(port, "コンテキスト");
            Network.LowerStackable ethlink = ethtype.getDemarcLink(port, true);
            if (ethlink.getCurrentUpperLayers(false).size() > 0) {
                throw new ShellCommandException("trunk 設定があるため解除できません.");
            }

            port.disjoinNetwork(ethlink);
        }

        private static <T extends Port> T validatePortAs(Port port, Class<T> klass, String messagePrefix)
            throws ShellCommandException
        {
            if (! (klass.isInstance(port))) {
                throw new ShellCommandException(
                    messagePrefix + "に" + SkeltonTefService.instance().uiTypeNames().getName(klass)
                    + "を指定してください.");
            }
            return klass.cast(port);
        }

        private static VlanLink getVlanDemarcationLink(VlanIf vlanif)
            throws ShellCommandException
        {
            VlanLink result = null;
            for (VlanLink link : vlanif.getHereafterNetworks(VlanLink.class)) {
                if (link.getCurrentMemberPorts().size() == 1) {
                    if (result == null) {
                        result = link;
                    } else {
                        throw new ShellCommandException("複数の境界リンクが見つかりました: " + vlanif.getFqn());
                    }
                }
            }
            return result;
        }

        private static VlanLink getVlanTrunkDemarcationLink(VlanIf vlanif, Port objectPort)
            throws ShellCommandException
        {
            VlanLink result = null;
            for (VlanLink link : vlanif.getHereafterNetworks(VlanLink.class)) {
                for (Network.LowerStackable lowerlayer : link.getHereafterLowerLayers(false)) {
                    if (lowerlayer.getCurrentMemberPorts().contains(objectPort)) {
                        if (result == null) {
                            result = link;
                        } else {
                            throw new IllegalStateException("複数の該当リンク: " + vlanif.getFqn());
                        }
                    }
                }
            }
            if (result != null && result.getCurrentMemberPorts().size() != 1) {
                throw new ShellCommandException(
                    "境界リンクではないリンクが接続されています: " + vlanif.getFqn() + " " + objectPort.getFqn());
            }
            return result;
        }
    }

    private enum EthType {

        ETH {

            @Override Network.LowerStackable getLink(Port port, boolean assertNotNull)
                throws ShellCommandException
            {
                EthLink result = selectLink(port, EthLink.class);
                if (assertNotNull && result == null) {
                    throw new ShellCommandException("eth-portにリンクが接続されていません.");
                }
                return result;
            }

            @Override EthLink getDemarcLink(Port port, boolean assertNotNull)
                throws ShellCommandException
            {
                EthLink result = selectDemarcLink(port, EthLink.class);
                if (assertNotNull && result == null) {
                    throw new ShellCommandException("eth-portに境界リンクが接続されていません.");
                }
                return result;
            }

            @Override EthLink newDemarcLink(Port port) {
                return new EthLink((EthPort) port, null);
            }
        },

        LAG {

            @Override Network.LowerStackable getLink(Port port, boolean assertNotNull)
                throws ShellCommandException
            {
                EthLag result = selectLink(port, EthLag.class);
                if (assertNotNull && result == null) {
                    throw new ShellCommandException("lag-portにリンクが接続されていません.");
                }
                return result;
            }

            @Override EthLag getDemarcLink(Port port, boolean assertNotNull)
                throws ShellCommandException
            {
                EthLag result = selectDemarcLink(port, EthLag.class);
                if (assertNotNull && result == null) {
                    throw new ShellCommandException("lag-portに境界リンクが接続されていません.");
                }
                return result;
            }

            @Override EthLag newDemarcLink(Port port) {
                return new EthLag((EthLagIf) port, null);
            }
        };

        abstract Network.LowerStackable getLink(Port port, boolean assertNotNull)
            throws ShellCommandException;
        abstract Network.LowerStackable getDemarcLink(Port port, boolean assertNotNull)
            throws ShellCommandException;
        abstract Network.LowerStackable newDemarcLink(Port port);

        static EthType get(Port port, String messagePrefix) throws ShellCommandException {
            if (port instanceof EthPort) {
                return ETH;
            } else if (port instanceof EthLagIf) {
                return LAG;
            } else {
                throw new ShellCommandException(
                    (messagePrefix == null ? "" : "に") + "eth-port/lag-portを指定してください.");
            }
        }
    }

    private static <T extends Network> T selectDemarcLink(Port port, Class<T> klass)
        throws ShellCommandException
    {
        Set<T> links = port.getHereafterNetworks(klass);
        if (links.size() == 0) {
            return null;
        } else if (links.size() == 1) {
            T result = links.iterator().next();
            if (result.getCurrentMemberPorts().size() != 1) {
                throw new ShellCommandException("境界リンクではないリンクが接続されています: " + port.getFqn());
            }
            return result;
        } else {
            throw new IllegalStateException("複数見つかりました: " + port.getFqn() + ", " + klass.getName());
        }
    }

    private static <T extends Network> T selectLink(Port port, Class<T> klass)
        throws ShellCommandException
    {
        Set<T> links = port.getHereafterNetworks(klass);
        if (links.size() == 0) {
            return null;
        } else if (links.size() == 1) {
            return links.iterator().next();
        } else {
            throw new ShellCommandException("複数見つかりました: " + port.getFqn() + ", " + klass.getName());
        }
    }

    @Override public String getArgumentDescription() {
        return "[operation] [port qualified name (operation optional)]";
    }

    @Override public void process(Commandline args) throws ShellCommandException {
        Port contextPort = contextAsPort();

        checkArgsSize(args, 1, 2);
        Operation op = resolveEnum("operation", Operation.class, args.arg(0));

        beginWriteTransaction();

        Port objectPort;
        if (op.needsObjectPort()) {
            checkArgsSize(args, 2);
            String portQualifiedName = op.needsObjectPort() ? args.arg(1) : null;

            String promptSuffix = getSession().getPromptSuffix();
            setContext(contextPort.getNode(), null);
            objectPort = resolve(Port.class, portQualifiedName);
            setContext(contextPort, promptSuffix);

            if (contextPort.getNode() != objectPort.getNode()) {
                throw new ShellCommandException("コンテキストportと指定portのnodeが異なります.");
            }
        } else {
            checkArgsSize(args, 1);

            objectPort = null;
        }

        op.process(contextPort, objectPort);

        commitTransaction();
    }
}
