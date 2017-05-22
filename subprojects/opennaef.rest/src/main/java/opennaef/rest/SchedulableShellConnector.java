package opennaef.rest;

import opennaef.rest.notifier.ScheduledNotifier;
import naef.ui.NaefShellFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.DateTime;
import tef.DateTimeFormat;
import tef.skelton.dto.DtoChanges;
import voss.core.server.builder.*;
import voss.core.server.exception.InventoryException;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

/**
 * @see {{@link voss.core.server.database.ShellConnector}}
 */
public final class SchedulableShellConnector {

    private static final Logger log = LoggerFactory.getLogger(SchedulableShellConnector.class);
    private static SchedulableShellConnector instance = null;

    public synchronized static SchedulableShellConnector getInstance() {
        if (instance == null) {
            instance = new SchedulableShellConnector();
        }
        return instance;
    }

    private SchedulableShellConnector() {
    }

    public synchronized NaefShellFacade getShellFacade() throws RemoteException {
        return NaefRmiConnector.instance().shellFacade();
    }


    /**
     * 複数の builderConfig を 1 transaction で実行する.
     *
     * @param targetTime
     * @param builder
     * @throws InventoryException
     */
    public synchronized DtoChanges execute(DateTime targetTime, CommandBuilder builder) throws InventoryException {
        List<CommandBuilder> builders = new ArrayList<>();
        builders.add(builder);
        return executes(targetTime, builders);
    }

    /**
     * 複数の builderConfig を 1 transaction で実行する.
     *
     * @param targetTime
     * @param builders
     * @throws InventoryException
     * @see {{@link voss.core.server.database.ShellConnector#executes(List)}}
     */
    public synchronized DtoChanges executes(DateTime targetTime, List<CommandBuilder> builders) throws InventoryException {
        try {
            List<String> assertions = new ArrayList<>();
            List<String> commands = new ArrayList<>();
            List<ComplementBuilder> complementBuilders = new ArrayList<>();
            int count = 1;

            commands.add("########## scheduled shell connector");
            commands.add("########## time: " + DateTimeFormat.YMDHMS_DOT.format(targetTime.toJavaDate()));
            commands.add("time " + targetTime.toString());

            for (CommandBuilder builder : builders) {
                if (builder == null) {
                    log.info("null builderConfig found. ignored.");
                    continue;
                }
                Commands cmd = builder.getCommand();
                if (cmd.isConditional()) {
                    cmd.evaluate();
                }
                if (builder.getBuildResult() == BuildResult.FAIL) {
                    log.info("build failed. ignored.");
                    continue;
                } else if (builder.getBuildResult() == BuildResult.NO_CHANGES) {
                    log.info("no changes, no updates.");
                    continue;
                }
                CommandUtil.showChanges(builder);
                checkBuilder(builder);
                assertions.add("#### " + count + " " + builder.toString());
                commands.add("#### " + count + " " + builder.toString());
                count++;
                assertions.addAll(cmd.getAssertions());
                commands.addAll(cmd.getRawCommands());
                commands.add(CMD.CONTEXT_RESET);
                complementBuilders.addAll(builder.getComplementBuilders());
            }
            List<String> allCommands = new ArrayList<>();
            allCommands.addAll(assertions);
            allCommands.addAll(commands);
            CommandUtil.logCommands(allCommands);

            // builderConfig のコマンドを実行
            NaefShellFacade shell = getShellFacade();
            DtoChanges dtoChanges = shell.executeBatch(allCommands);

            //
            for (ComplementBuilder complement : complementBuilders) {
                BuildResult r = complement.buildCommand();
                if (BuildResult.SUCCESS != r) {
                    log.warn("complement-builderConfig ignored: " + r);
                    continue;
                }
                execute(targetTime, complement);
            }

            // ScheduledNotifier へ変更を登録する
            ScheduledNotifier.instance().add(targetTime, dtoChanges);
            return dtoChanges;

        } catch (NaefShellFacade.ShellException e) {
            Integer errorLineNumber = e.getBatchLineCount();
            String errorLineContent = e.getCommand();
            throw new InventoryException("更新に失敗しました: [" + errorLineNumber + "] '" + errorLineContent + "'", e);
        } catch (Exception e) {
            throw new InventoryException("更新に失敗しました.", e);
        }
    }

    /**
     * builderConfig が実行可能かチェックする.
     */
    private static void checkBuilder(CommandBuilder builder) throws InventoryException {
        if (builder.executed()) {
            throw new InventoryException("already executed.");
        } else if (builder.getBuildResult() == null) {
            throw new InventoryException("not built.");
        }
    }
}
