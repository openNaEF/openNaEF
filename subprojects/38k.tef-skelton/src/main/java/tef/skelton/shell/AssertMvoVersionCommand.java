package tef.skelton.shell;

import tef.MVO;
import tef.TefService;
import tef.TransactionId;
import tef.skelton.AbstractModel;

public class AssertMvoVersionCommand extends SkeltonShellCommand {

    @Override public String getArgumentDescription() {
        return "[mvo id] [version]";
    }

    @Override public void process(Commandline args) throws ShellCommandException {
        checkArgsSize(args, 2);
        MVO.MvoId mvoId = MVO.MvoId.getInstanceByLocalId(args.arg(0));
        TransactionId.W version = (TransactionId.W) TransactionId.getInstance(args.arg(1));

        beginReadTransaction();

        MVO mvo = TefService.instance().getMvoRegistry().get(mvoId);
        if (mvo == null) {
            throw new ShellCommandException("オブジェクトが見つかりません: " + mvoId);
        }

        TransactionId.W objectVersion;
        if (mvo instanceof AbstractModel) {
            objectVersion = ((AbstractModel) mvo).getExtendedVersion();
        } else {
            objectVersion = mvo.getLatestVersion();
        }

        if (objectVersion.serial > version.serial) {
            throw new ShellCommandException(mvo.getMvoId() + " は更新されています.");
        }
    }
}
