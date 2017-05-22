package voss.discovery.agent.util;

import voss.discovery.iolib.simulation.SimulationArchive;
import voss.discovery.iolib.simulation.SimulationArchiveImpl;
import voss.discovery.iolib.simulation.SimulationEntry;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipOutputStream;

public class DeviceRecorder {
    private final SimulationArchive archive;
    private final ZipOutputStream zos;

    public DeviceRecorder(String recordingSessionName) throws IOException {
        if (recordingSessionName == null) {
            throw new IllegalArgumentException("session-name is null.");
        }
        this.archive = new SimulationArchiveImpl(recordingSessionName);
        this.zos = this.archive.open();
    }

    public DeviceRecorder(File dir, String recordingSessionName) throws IOException {
        if (recordingSessionName == null) {
            throw new IllegalArgumentException("session-name is null.");
        } else if (dir == null) {
            throw new IllegalArgumentException("dir is null.");
        }
        this.archive = new SimulationArchiveImpl(dir, recordingSessionName);
        this.zos = this.archive.open();
    }

    public void close() throws IOException {
        if (this.archive.isOpened()) {
            this.archive.close();
        }
    }

    public SimulationEntry addEntry(String targetId) throws IOException {
        SimulationEntry entry = this.archive.addSimulationEntry(zos, targetId);
        return entry;
    }

}