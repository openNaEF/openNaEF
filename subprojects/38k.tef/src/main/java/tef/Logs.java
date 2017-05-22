package tef;

import lib38k.io.IoUtils;
import lib38k.logger.FileLogger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;

class Logs {

    private final long time_;
    private final File logsDirectory_;

    Logs(long time, File workingDirectory, String logsDirectoryName) {
        time_ = time;
        logsDirectory_ = IoUtils.initializeDirectory(workingDirectory, logsDirectoryName);
    }

    File getLogsDirectory() {
        return logsDirectory_;
    }

    FileLogger createLogger(String logDirName) {
        File logFile = createLogFile(logDirName);
        try {
            return new FileLogger(logFile);
        } catch (FileNotFoundException fnfe) {
            throw new Error(fnfe.getMessage());
        }
    }

    private File createLogFile(String logDirName) {
        File logDir = IoUtils.initializeDirectory(getLogsDirectory(), logDirName);
        String logFileName = TefUtils.formatDateYyyymmddhhmmss(new Date(time_)) + ".log";
        File logFile = new File(logDir, logFileName);
        if (logFile.exists()) {
            throw new Error("file already exists: " + logFile.getAbsolutePath());
        }

        try {
            logFile.createNewFile();
        } catch (IOException ioe) {
            throw new Error
                    ("log file creation failed: " + logFile.getAbsolutePath(), ioe);
        }

        return logFile;
    }
}
