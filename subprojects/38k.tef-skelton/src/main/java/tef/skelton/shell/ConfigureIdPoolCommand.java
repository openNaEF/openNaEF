package tef.skelton.shell;

import java.util.List;

import tef.skelton.FormatException;
import tef.skelton.IdPool;
import tef.skelton.Range;

public class ConfigureIdPoolCommand extends SkeltonShellCommand {

    private enum Operation {

        ALLOCATE_RANGE(1) {

            @Override void process(IdPool pool, List<String> args) throws ShellCommandException {
                String rangeStr = args.get(0);

                try {
                    pool.allocateRange(pool.parseRange(rangeStr));
                } catch (FormatException fe) {
                    throw new ShellCommandException(fe.getMessage());
                } catch (IdPool.PoolException pe) {
                    throw new ShellCommandException(pe.getMessage());
                }
            }
        },

        RELEASE_RANGE(1) {

            @Override void process(IdPool pool, List<String> args) throws ShellCommandException {
                String rangeStr = args.get(0);

                try {
                    pool.releaseRange(pool.parseRange(rangeStr));
                } catch (FormatException fe) {
                    throw new ShellCommandException(fe.getMessage());
                } catch (IdPool.PoolException pe) {
                    throw new ShellCommandException(pe.getMessage());
                }
            }
        },

        ALTER_RANGE(2) {

            @Override void process(IdPool pool, List<String> args) throws ShellCommandException {
                String existingRangeStr = args.get(0);
                String newRangeStr = args.get(1);

                try {
                    pool.alterRange(pool.parseRange(existingRangeStr), pool.parseRange(newRangeStr));
                } catch (FormatException fe) {
                    throw new ShellCommandException(fe.getMessage());
                } catch (IdPool.PoolException pe) {
                    throw new ShellCommandException(pe.getMessage());
                }
            }
        },

        MERGE_RANGE(2) {

            @Override void process(IdPool pool, List<String> args) throws ShellCommandException {
                String existingRange1Str = args.get(0);
                String existingRange2Str = args.get(1);

                try {
                    pool.mergeRange(pool.parseRange(existingRange1Str), pool.parseRange(existingRange2Str));
                } catch (FormatException fe) {
                    throw new ShellCommandException(fe.getMessage());
                } catch (IdPool.PoolException pe) {
                    throw new ShellCommandException(pe.getMessage());
                }
            }
        },

        SPLIT_RANGE(2) {

            @Override void process(IdPool pool, List<String> args) throws ShellCommandException {
                try {
                    String existingRangeStr = args.get(0);
                    String[] splitterRangeTokens = Range.tokenizeRangeStr(args.get(1));

                    Object splitterLower = pool.parseId(splitterRangeTokens[0]);
                    Object splitterUpper = pool.parseId(splitterRangeTokens[1]);
                    Range.checkOrder((Comparable) splitterLower, (Comparable) splitterUpper);

                    pool.splitRange(pool.parseRange(existingRangeStr), splitterLower, splitterUpper);
                } catch (FormatException fe) {
                    throw new ShellCommandException(fe.getMessage());
                } catch (IdPool.PoolException pe) {
                    throw new ShellCommandException(pe.getMessage());
                }
            }
        };

        private final int argsSize;

        Operation(int argsSize) {
            this.argsSize = argsSize;
        }

        abstract void process(IdPool pool, List<String> args) throws ShellCommandException;
    }

    @Override public String getArgumentDescription() {
        return "[operation] [args]..";
    }

    @Override public void process(Commandline args) throws ShellCommandException {
        IdPool pool = contextAs(IdPool.class, "IDプール");

        checkArgsSize(args, 1, Integer.MAX_VALUE);
        Operation op = resolveEnum("operation", Operation.class, args.arg(0));

        beginWriteTransaction();

        checkArgsSize(args, op.argsSize + 1);
        op.process(pool, args.args().subList(1, args.args().size()));

        commitTransaction();
    }
}
