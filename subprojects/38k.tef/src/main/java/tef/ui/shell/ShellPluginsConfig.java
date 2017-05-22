package tef.ui.shell;

import lib38k.plugin.PluginUtils;
import lib38k.plugin.PluginsConfig;
import tef.TefFileUtils;
import tef.TefService;

import java.io.File;
import java.util.*;
import java.util.zip.ZipFile;

public class ShellPluginsConfig extends PluginsConfig<ShellCommand> {

    private static final String CONFIG_FILE_NAME = "TefShellPluginsConfig.xml";

    private static final ShellPluginsConfig instance__ = new ShellPluginsConfig();

    private LinkedHashMap<String, Class<? extends ShellCommand>> prototypes_
            = new LinkedHashMap<String, Class<? extends ShellCommand>>();

    public static ShellPluginsConfig getInstance() {
        return instance__;
    }

    private ShellPluginsConfig() {
        super
                (ShellCommand.class,
                        new File(TefService.instance().getConfigsDirectory(), CONFIG_FILE_NAME),
                        TefFileUtils.isAbsolutePath(pluginFileName())
                                ? new File(pluginFileName())
                                : new File(TefService.instance().getWorkingDirectory(), pluginFileName()));
    }

    private static String pluginFileName() {
        return TefService.instance().getTefServiceConfig().shellConfig.pluginFileName;
    }

    @Override
    protected void update
            (ZipFile pluginsFile, List<PluginConfigEntry<ShellCommand>> configEntries)
            throws UpdateException {
        synchronized (this) {
            prototypes_.clear();

            for (PluginConfigEntry<ShellCommand> entry : configEntries) {
                if (prototypes_.get(entry.pluginName) != null) {
                    throw new UpdateException
                            ("duplicated plug-in name: " + entry.pluginName);
                }

                try {
                    entry.prototype.newInstance();
                } catch (Exception e) {
                    throw new UpdateException(e);
                }

                prototypes_.put(entry.pluginName, entry.prototype);
            }

            ShellServer.getInstance().getLogger()
                    .log("plug-in updated.\t" + PluginUtils.getVersion(pluginsFile));

            List<String> commandnames = new ArrayList<String>();
            for (String commandname : prototypes_.keySet()) {
                commandnames.add(commandname + "\t" + prototypes_.get(commandname).getName());
            }
            ShellServer.getInstance().getLogger()
                    .log(prototypes_.size() + " commands:", commandnames);
        }
    }

    synchronized Map<String, Class<? extends ShellCommand>> getCommandPrototypes() {
        return Collections.<String, Class<? extends ShellCommand>>unmodifiableMap(prototypes_);
    }

    synchronized boolean isUpToDate(Collection<ShellCommand> commands) {
        Set<Class<?>> existingCommands = new HashSet<Class<?>>(prototypes_.values());
        for (ShellCommand command : commands) {
            if (!existingCommands.contains(command.getClass())) {
                return false;
            }
        }
        return true;
    }
}
