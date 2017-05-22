package tef;

import lib38k.io.IoUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

class JournalArchivingService {

    private static final long MAX_INTERVAL = 10 * 60 * 1000l;

    private final TefService tefService_;

    private final File backupTransactionsDir_;

    JournalArchivingService(TefService tefService) throws IOException {
        tefService_ = tefService;

        backupTransactionsDir_
                = IoUtils.initializeDirectory
                (IoUtils.initializeDirectory
                                (tefService_.getWorkingDirectory(),
                                        "backup"),
                        "transactions");

        archive(System.currentTimeMillis());
    }

    void start() {
        new Thread() {

            @Override
            public void run() {
                mainLoop();
            }
        }.start();
    }

    private void mainLoop() {
        try {
            while (true) {
                long nextTime = getTomorrow0oclock().getTime();

                while (System.currentTimeMillis() < nextTime) {
                    long timeToSleep
                            = Math.min(MAX_INTERVAL, nextTime - System.currentTimeMillis() + 1000);
                    try {
                        Thread.sleep(timeToSleep);
                    } catch (InterruptedException ie) {
                    }
                }

                archive(nextTime);
            }
        } catch (Throwable t) {
            logError("journal archiving service has unexpectedly stopped.", t);
        }
    }

    private Date getTomorrow0oclock() {
        GregorianCalendar calendar = new GregorianCalendar();

        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        calendar.add(Calendar.DAY_OF_YEAR, 1);

        return calendar.getTime();
    }

    private void archive(long dateDirThreshold) throws IOException {
        File journalsDir = tefService_.getLogs().getJournalsDirectory();

        List<File> journalDirs = new ArrayList<File>();
        for (File dir : TefFileUtils.getDirectories(journalsDir)) {
            Long directoryNameParsedAsDate = JournalFileUtils.parseDate(dir.getName());
            if (directoryNameParsedAsDate == null
                    || directoryNameParsedAsDate.longValue() < dateDirThreshold) {
                journalDirs.add(dir);
            }
        }
        if (journalDirs.size() == 0) {
            logMessage("dirs:" + journalDirs.size());
            return;
        }

        String id = TefUtils.formatDateYyyymmddhhmmss(new java.util.Date());
        logMessage("dirs:" + journalDirs.size() + ", archive:" + id);

        File archiveFile = new File(journalsDir, id + "-archive.tmp");
        if (archiveFile.exists()) {
            throw new IllegalStateException();
        }

        ZipOutputStream archiveOut = null;
        archiveOut = new ZipOutputStream
                (new BufferedOutputStream(new FileOutputStream(archiveFile, false)));

        try {
            for (File directory : journalDirs) {
                for (File file : directory.listFiles()) {
                    if (file.isDirectory()) {
                        throw new IllegalStateException
                                ("nested directory found: " + file.getAbsolutePath());
                    }
                    if (!file.isFile()) {
                        throw new IllegalStateException
                                ("not a file: " + file.getAbsolutePath());
                    }

                    ZipEntry fileEntry
                            = new ZipEntry(directory.getName() + "/" + file.getName());
                    fileEntry.setTime(file.lastModified());
                    archiveOut.putNextEntry(fileEntry);
                    archiveOut.write(IoUtils.readFile(file));
                    archiveOut.closeEntry();
                }
            }
        } finally {
            archiveOut.close();
        }

        File renameTo = new File(journalsDir, id + ".zip");
        boolean renameSuccessed = archiveFile.renameTo(renameTo);
        if (!renameSuccessed) {
            throw new RuntimeException
                    ("renaming archive file has failed: " + archiveFile.getAbsolutePath());
        }

        File backupDir = getBackupDir(id, null);
        logMessage("moving originals to " + backupDir.getAbsolutePath());
        for (File directory : journalDirs) {
            File destinationDir = new File(backupDir, directory.getName());
            boolean moveSuccessed = directory.renameTo(destinationDir);
            if (!moveSuccessed) {
                throw new RuntimeException
                        ("moving original journals has failed: " + directory.getAbsolutePath());
            }
        }

        logMessage("archiving completed.");
    }

    private File getBackupDir(String id, Integer serial) {
        String suffix = serial == null ? "" : "-" + serial.toString();
        File result = new File(backupTransactionsDir_, id + suffix);
        if (result.exists()) {
            return getBackupDir(id, new Integer(serial == null ? 1 : serial.intValue() + 1));
        } else {
            result.mkdirs();
            return result;
        }
    }

    private void logMessage(String message) {
        tefService_.logMessage("[journal archiving service]" + message);
    }

    private void logError(String message, Throwable t) {
        tefService_.logError("[journal archiving service]" + message, t);
    }
}
