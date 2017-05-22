package tef.ui.shell;

import tef.*;

import java.io.IOException;

public class MvoDump extends ShellCommand {

    public String getArgumentDescription() {
        return "[target (full|oid)] [last transaction-id (optional)]";
    }

    @Override
    public void process(Commandline commandline) throws ShellCommandException {
        TransactionId.W lastTransactionId;
        if (commandline.args().size() == 1) {
            lastTransactionId = null;
        } else if (commandline.args().size() == 2) {
            lastTransactionId
                    = (TransactionId.W) TransactionId.getInstance(commandline.arg(1));
        } else {
            println(getCommandUsage());
            return;
        }

        beginReadTransaction();
        try {
            String targetStr = commandline.arg(0);
            MVO targetMvo;
            if (targetStr.equals("full")) {
                targetMvo = null;
                MvoDumper.fullDump(lastTransactionId);
            } else {
                MVO.MvoId mvoId = MVO.MvoId.getInstanceByLocalId(targetStr);
                targetMvo = TefService.instance().getMvoRegistry().get(mvoId);
                if (targetMvo == null) {
                    throw new ShellCommandException("no such object.");
                }

                MvoDumper.MvoDumpStream dumpStream
                        = new MvoDumper.DefaultMvoDumpStream(getPrintStream());
                MvoDumper.dump(dumpStream, lastTransactionId, targetMvo);
            }
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        } finally {
            TransactionContext.close();
        }
    }
}
