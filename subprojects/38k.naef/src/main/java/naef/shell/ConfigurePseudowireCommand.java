package naef.shell;

import naef.mvo.Port;
import naef.mvo.mpls.Pseudowire;

public class ConfigurePseudowireCommand extends NaefShellCommand {

    private enum Operation {

        SET_AC1, SET_AC2, RESET_AC1, RESET_AC2
    }

    @Override public String getArgumentDescription() {
        return "[operation: set-ac1/set-ac2/reset-ac1/reset-ac2] [extra arg]";
    }

    @Override public void process(Commandline args) throws ShellCommandException {
        checkArgsSize(args, 1, 3);

        Operation op = resolveEnum("operation", Operation.class, args.arg(0));

        beginWriteTransaction();

        switch(op) {
            case SET_AC1: {
                checkArgsSize(args, 2);
                Pseudowire pseudowire = contextAs(Pseudowire.class, "pseudowire");
                String portQualifiedName = args.arg(1);

                pseudowire.setAttachmentCircuit1(resolve(Port.class, portQualifiedName));
                break;
            }
            case SET_AC2: {
                checkArgsSize(args, 2);
                Pseudowire pseudowire = contextAs(Pseudowire.class, "pseudowire");
                String portQualifiedName = args.arg(1);

                pseudowire.setAttachmentCircuit2(resolve(Port.class, portQualifiedName));
                break;
            }
            case RESET_AC1: {
                checkArgsSize(args, 1);
                Pseudowire pseudowire = contextAs(Pseudowire.class, "pseudowire");

                pseudowire.resetAttachmentCircuit1();
                break;
            }
            case RESET_AC2: {
                checkArgsSize(args, 1);
                Pseudowire pseudowire = contextAs(Pseudowire.class, "pseudowire");

                pseudowire.resetAttachmentCircuit2();
                break;
            }
            default:
                throw new RuntimeException();
        }

        commitTransaction();
    }
}
