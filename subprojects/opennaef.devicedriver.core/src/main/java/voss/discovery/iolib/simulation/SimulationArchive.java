package voss.discovery.iolib.simulation;

import voss.model.NodeInfo;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.zip.ZipOutputStream;

public interface SimulationArchive {

    public abstract ZipOutputStream open() throws IOException;

    public abstract boolean isOpened();

    public abstract void close() throws IOException;

    public abstract SimulationEntry getSimulationEntry(NodeInfo nodeinfo)
            throws IOException;

    public abstract SimulationEntry addSimulationEntry(ZipOutputStream zos,
                                                       String target) throws IOException;

    public abstract boolean exists(String target);

    public abstract File getSimulationArchiveFile();

    public String getTargetBy(InetAddress inetAddress);
}