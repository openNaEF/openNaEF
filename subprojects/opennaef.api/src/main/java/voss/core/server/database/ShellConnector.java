package voss.core.server.database;

import naef.ui.NaefShellFacade;
import naef.ui.NaefShellFacade.ShellException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.builder.*;
import voss.core.server.config.CoreConfiguration;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ShellConnector {
    private static final Logger log = LoggerFactory.getLogger(ShellConnector.class);
    private static ShellConnector instance = null;

    public synchronized static ShellConnector getInstance() {
        if (instance == null) {
            instance = new ShellConnector();
        }
        return instance;
    }

    CoreConfiguration config = null;

    protected ShellConnector() {
        this.config = CoreConfiguration.getInstance();
    }

    public synchronized NaefShellFacade getShellFacade() throws IOException, RemoteException, ExternalServiceException {
        return this.config.getUpdateServerBridge().getShellFacade();
    }

    public synchronized void execute(CommandBuilder builder) throws InventoryException {
        List<CommandBuilder> builders = new ArrayList<CommandBuilder>();
        builders.add(builder);
        executes(builders);
    }

    public synchronized void executes(List<CommandBuilder> builders)
            throws InventoryException {
        try {
            List<String> assertions = new ArrayList<String>();
            List<String> commands = new ArrayList<String>();
            List<ComplementBuilder> complementBuilders = new ArrayList<ComplementBuilder>();
            int count = 1;
            for (CommandBuilder builder : builders) {
                if (builder == null) {
                    log.info("null builder found. ignored.");
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
            List<String> allCommands = new ArrayList<String>();
            allCommands.addAll(assertions);
            allCommands.addAll(commands);
            NaefShellFacade shell = getShellFacade();
            CommandUtil.logCommands(allCommands);
            shell.executeBatch(allCommands);
            for (ComplementBuilder complement : complementBuilders) {
                BuildResult r = complement.buildCommand();
                if (BuildResult.SUCCESS != r) {
                    log.warn("complement-builder ignored: " + r);
                    continue;
                }
                execute(complement);
            }
        } catch (ShellException e) {
            Integer errorLineNumber = e.getBatchLineCount();
            String errorLineContent = e.getCommand();
            throw new InventoryException("Update failed: [" + errorLineNumber + "] '" + errorLineContent + "'", e);
        } catch (Exception e) {
            throw new InventoryException("Update failed.", e);
        }
    }

    public synchronized void executes(CommandBuilder... builders) throws InventoryException {
        List<CommandBuilder> list = Arrays.asList(builders);
        executes(list);
    }

    public synchronized void execute2(Commands cmd) throws InventoryException {
        List<Commands> commands = new ArrayList<Commands>();
        commands.add(cmd);
        executes2(commands);
    }

    public synchronized void executes2(Commands... commands) throws InventoryException {
        List<Commands> list = Arrays.asList(commands);
        executes2(list);
    }

    public synchronized void executes2(List<? extends Commands> cmds) throws InventoryException {
        try {
            NaefShellFacade shell = getShellFacade();
            List<String> assertions = new ArrayList<String>();
            List<String> commands = new ArrayList<String>();
            int count = 1;
            for (Commands cmd : cmds) {
                if (cmd == null) {
                    continue;
                }
                assertions.add("#### " + count);
                commands.add("#### " + count);
                count++;
                if (cmd.isConditional()) {
                    cmd.evaluate();
                }
                assertions.addAll(cmd.getAssertions());
                commands.addAll(cmd.getRawCommands());
                commands.add(CMD.CONTEXT_RESET);
            }
            List<String> merged = new ArrayList<String>();
            merged.addAll(assertions);
            merged.addAll(commands);
            CommandUtil.logCommands(merged);
            shell.executeBatch(merged);
        } catch (ShellException e) {
            Integer errorLineNumber = e.getBatchLineCount();
            String errorLineContent = e.getCommand();
            throw new InventoryException("Update failed: [" + errorLineNumber + "] '" + errorLineContent + "'", e);
        } catch (Exception e) {
            throw new InventoryException("Update failed.", e);
        }
    }

    private static void checkBuilder(CommandBuilder builder) throws InventoryException {
        if (builder.executed()) {
            throw new InventoryException("already executed.");
        } else if (builder.getBuildResult() == null) {
            throw new InventoryException("not built.");
        }
    }

}