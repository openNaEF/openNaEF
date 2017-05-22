package naef.shell;

import naef.mvo.CrossConnection;
import naef.mvo.NetworkUtils;
import naef.mvo.Port;
import tef.skelton.ConfigurationException;
import tef.skelton.ResolveException;
import tef.skelton.ValueException;

public class ConfigurePortInterconnectionCommand extends NaefShellCommand {

    private enum Operation {

        X_CONNECT {

            @Override void process(Port port1, Port port2)
                throws ResolveException, ShellCommandException
            {
                if (NetworkUtils.resolveHereafterNetwork(CrossConnection.class, port1, port2) != null) {
                    throw new ShellCommandException("既に接続済です.");
                }

                new CrossConnection(port1, port2);
            }
        },

        X_DISCONNECT {

            @Override void process(Port port1, Port port2)
                throws ResolveException, ShellCommandException
            {
                CrossConnection xcon = NetworkUtils.resolveHereafterNetwork(CrossConnection.class, port1, port2);
                if (xcon == null) {
                    throw new ShellCommandException("接続されていません.");
                }

                xcon.disconnect();
            }
        },

        STACK {

            @Override void process(Port lower, Port upper) {
                lower.addUpperLayerPort(upper);
                upper.addLowerLayerPort(lower);
            }
        },

        UNSTACK {

            @Override void process(Port lower, Port upper) {
                lower.removeUpperLayerPort(upper);
                upper.removeLowerLayerPort(lower);
            }
        },

        INCLUDE {

            @Override void process(Port outer, Port inner) {
                outer.addPart(inner);
                inner.setContainer(outer);
            }
        },

        EXCLUDE {

            @Override void process(Port outer, Port inner) {
                outer.removePart(inner);
                inner.resetContainer();
            }
        };

        abstract void process(Port port1, Port port2)
            throws ResolveException, ShellCommandException;
    }

    @Override public String getArgumentDescription() {
        return "[operation] [port1/lower/outer] [port2/upper/inner]";
    }

    @Override public void process(Commandline args) throws ShellCommandException {
        checkArgsSize(args, 3);
        Operation op = resolveEnum("operation", Operation.class, args.arg(0));
        String port1Desc = args.arg(1);
        String port2Desc = args.arg(2);

        beginWriteTransaction();

        Port port1 = resolve(Port.class, port1Desc);
        Port port2 = resolve(Port.class, port2Desc);

        if (port1.getNode() != port2.getNode()) {
            throw new ShellCommandException("port1 と port2 の node が異なります.");
        }

        try {
            op.process(port1, port2);
        } catch (ResolveException re) {
            throw new ShellCommandException(re.getMessage());
        } catch (ValueException ve) {
            throw new ShellCommandException(ve.getMessage());
        } catch (ConfigurationException ce) {
            throw new ShellCommandException(ce.getMessage());
        }

        commitTransaction();
    }
}
